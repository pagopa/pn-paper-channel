package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.SendProgressMetaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RECAG012ChainedMessageHandler {
    private EventMetaDAO mockEventMetaDAO;
    private EventDematDAO mockEventDematDAO;
    private SqsSender mockSqsSender;
    private RequestDeliveryDAO mockRequestDeliveryDAO;
    private PnEventErrorDAO mockPnEventErrorDAO;
    private SendProgressMetaConfig mockSendProgressMetaConfig;
    private SaveMetadataMessageHandler saveMetadataMessageHandler;
    private HandlersFactory handlersFactory;

    @BeforeEach
    public void init(){
        PnPaperChannelConfig mockConfig = mock(PnPaperChannelConfig.class);
        mockEventMetaDAO = mock(EventMetaDAO.class);
        mockSqsSender = mock(SqsSender.class);
        mockRequestDeliveryDAO = mock(RequestDeliveryDAO.class);
        mockPnEventErrorDAO = mock(PnEventErrorDAO.class);
        mockSendProgressMetaConfig = mock(SendProgressMetaConfig.class);
        mockEventDematDAO = mock(EventDematDAO.class);

        long ttlDays = 365;
        when(mockConfig.getTtlExecutionDaysMeta()).thenReturn(ttlDays);
        when(mockConfig.isEnableSimple890Flow()).thenReturn(true);
        when(mockConfig.getRequiredDemats()).thenReturn(Set.of("23L"));

        saveMetadataMessageHandler = SaveMetadataMessageHandler.builder()
                .eventMetaDAO(mockEventMetaDAO)
                .pnPaperChannelConfig(mockConfig)
                .build();
        handlersFactory = new HandlersFactory(null, null, null,
                mockConfig, mockSqsSender, mockEventMetaDAO, mockEventDematDAO, null,
                mockRequestDeliveryDAO, mockPnEventErrorDAO, mockSendProgressMetaConfig, null,
                null, null);
    }

    @Test
    void handleMessage_ok_no23L_metaEnabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(true);
        when(mockSendProgressMetaConfig.isRECAG012AEnabled()).thenReturn(true);
        handlersFactory.initializeHandlers();

        MessageHandler handler = handlersFactory.getHandler(ExternalChannelCodeEnum.RECAG012.name());

        // Arrange
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        paperRequest.setStatusDateTime(now);

        // Send event
        PnDeliveryRequest sendEntity = new PnDeliveryRequest();
        sendEntity.setRequestId("requestId");
        sendEntity.setStatusCode(ExternalChannelCodeEnum.RECAG012A.name());
        sendEntity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        sendEntity.setStatusDescription(ExternalChannelCodeEnum.RECAG012A.name());

        PaperProgressStatusEventDto sendPaperRequest = new PaperProgressStatusEventDto();
        sendPaperRequest.setRequestId(entity.getRequestId());
        sendPaperRequest.setStatusCode(ExternalChannelCodeEnum.RECAG012A.name());
        sendPaperRequest.setStatusDateTime(now);

        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(sendEntity, sendPaperRequest);

        PnEventMeta pnEventMeta = saveMetadataMessageHandler.buildPnEventMeta(paperRequest);
        when(mockEventMetaDAO.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));
        when(mockEventMetaDAO.getDeliveryEventMeta(anyString(),anyString())).thenReturn(Mono.empty());
        when(mockRequestDeliveryDAO.updateData(any(), anyBoolean())).thenReturn(Mono.just(entity));
        when(mockEventDematDAO.findAllByRequestId(anyString(), anyBoolean())).thenReturn(Flux.empty());

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // Assert
        // I expect entity and paperRequest to be changed
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), entity.getStatusCode());
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), entity.getStatusDescription());
        assertEquals(StatusCodeEnum.PROGRESS.getValue(), entity.getStatusDetail());
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), paperRequest.getStatusCode());
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), paperRequest.getStatusDescription());

        // I expect it to save the event
        verify(mockEventMetaDAO, times(1)).createOrUpdate(pnEventMeta);

        // I expect it to send the message to delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent( any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId( anyString());

        // Not called because it is a PROGRESS event
        verify(mockRequestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(anyString(), any(Instant.class));
    }

    @Test
    void handleMessage_ok_no23L_metaDisabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(false);
        when(mockSendProgressMetaConfig.isRECAG012AEnabled()).thenReturn(false);
        handlersFactory.initializeHandlers();

        MessageHandler handler = handlersFactory.getHandler(ExternalChannelCodeEnum.RECAG012.name());

        // Arrange
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        paperRequest.setStatusDateTime(now);

        PnEventMeta pnEventMeta = saveMetadataMessageHandler.buildPnEventMeta(paperRequest);
        when(mockEventMetaDAO.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));
        when(mockEventMetaDAO.getDeliveryEventMeta(anyString(),anyString())).thenReturn(Mono.empty());
        when(mockRequestDeliveryDAO.updateData(any(), anyBoolean())).thenReturn(Mono.just(entity));
        when(mockEventDematDAO.findAllByRequestId(anyString(), anyBoolean())).thenReturn(Flux.empty());

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // Assert
        // I expect entity and paperRequest to be changed
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), entity.getStatusCode());
        assertEquals(StatusCodeEnum.PROGRESS.getValue(), entity.getStatusDetail());
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), paperRequest.getStatusCode());

        // I expect it to save the event
        verify(mockEventMetaDAO, times(1)).createOrUpdate(pnEventMeta);

        // I expect it not to send the message to delivery-push
        verify(mockSqsSender, never()).pushSendEvent(any());
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent( any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId( anyString());

        // Not called because it is a PROGRESS event
        verify(mockRequestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(anyString(), any(Instant.class));
    }

    @Test
    void handleMessage_ok_23L_metaEnabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(true);
        when(mockSendProgressMetaConfig.isRECAG012AEnabled()).thenReturn(true);
        handleMessage_ok_23L();
    }

    @Test
    void handleMessage_ok_23L_metaDisabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(false);
        when(mockSendProgressMetaConfig.isRECAG012AEnabled()).thenReturn(false);
        handleMessage_ok_23L();
    }

    void handleMessage_ok_23L() {
        handlersFactory.initializeHandlers();

        MessageHandler handler = handlersFactory.getHandler(ExternalChannelCodeEnum.RECAG012.name());

        // Arrange
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        paperRequest.setStatusDateTime(now);

        // Send event
        // pnDeliveryRequestPNAG012
        PnDeliveryRequest sendEntity = new PnDeliveryRequest();
        sendEntity.setRequestId("requestId");
        sendEntity.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        sendEntity.setStatusDetail(StatusCodeEnum.OK.getValue());
        // delayedRECAG012Event
        PaperProgressStatusEventDto sendPaperRequest = new PaperProgressStatusEventDto();
        sendPaperRequest.setRequestId(entity.getRequestId());
        sendPaperRequest.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        sendPaperRequest.setStatusDateTime(now);

        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(sendEntity, sendPaperRequest);

        PnEventMeta pnEventMeta = saveMetadataMessageHandler.buildPnEventMeta(paperRequest);
        when(mockEventMetaDAO.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));
        when(mockEventMetaDAO.getDeliveryEventMeta(anyString(),anyString()))
                .thenReturn(Mono.empty());

        when(mockRequestDeliveryDAO.updateConditionalOnFeedbackStatus(any(), anyBoolean()))
                .thenReturn(Mono.just(entity));

        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("23L");
        when(mockEventDematDAO.findAllByRequestId(anyString(), anyBoolean()))
                .thenReturn(Flux.just(pnEventDemat));

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // Assert
        // I expect entity and paperRequest not to be changed
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), entity.getStatusCode());
        assertEquals(StatusCodeEnum.PROGRESS.getValue(), entity.getStatusDetail());
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), paperRequest.getStatusCode());

        // I expect it to save the event
        verify(mockEventMetaDAO, times(1)).createOrUpdate(pnEventMeta);

        // I expect it not to send the message to delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent( any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId( anyString());

        // Update delivery request to track feedback of RECAGSimplifiedPostLogicHandler
        verify(mockRequestDeliveryDAO, times(1))
                .updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(anyString(), any(Instant.class));
    }
}
