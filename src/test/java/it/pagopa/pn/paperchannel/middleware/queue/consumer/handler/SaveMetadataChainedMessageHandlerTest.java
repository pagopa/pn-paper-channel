package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.SendProgressMetaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SaveMetadataChainedMessageHandlerTest {
    private EventMetaDAO mockEventMetaDAO;
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

        long ttlDays = 365;
        when(mockConfig.getTtlExecutionDaysMeta()).thenReturn(ttlDays);

        saveMetadataMessageHandler = SaveMetadataMessageHandler.builder()
                .eventMetaDAO(mockEventMetaDAO)
                .pnPaperChannelConfig(mockConfig)
                .build();
        handlersFactory = new HandlersFactory(null, null, null,
                mockConfig, mockSqsSender, mockEventMetaDAO, null, null,
                mockRequestDeliveryDAO, mockPnEventErrorDAO, mockSendProgressMetaConfig);
    }

    @Test
    void handleMessage_ok_metaEnabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(true);
        handlersFactory.initializeHandlers();

        MessageHandler handler = handlersFactory.getHandler(ExternalChannelCodeEnum.RECRS002A.name());

        // Arrange
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("some");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        PnEventMeta pnEventMeta = saveMetadataMessageHandler.buildPnEventMeta(paperRequest);
        when(mockEventMetaDAO.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));

        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // Assert
        // I expect it to save the event
        verify(mockEventMetaDAO, times(1)).createOrUpdate(pnEventMeta);

        // I expect it to send the message to delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent( any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId( anyString());

        // Not called because it is a PROGRESS event
        verify(mockRequestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(anyString(),  any(Instant.class));
    }

    @Test
    void handleMessage_ok_metaDisabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(false);
        handlersFactory.initializeHandlers();

        MessageHandler handler = handlersFactory.getHandler(ExternalChannelCodeEnum.RECRS002A.name());

        // Arrange
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode(ExternalChannelCodeEnum.RECRS002A.name());
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        PnEventMeta pnEventMeta = saveMetadataMessageHandler.buildPnEventMeta(paperRequest);
        when(mockEventMetaDAO.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        // Assert
        // I expect it to save the event
        verify(mockEventMetaDAO, times(1)).createOrUpdate(pnEventMeta);

        // I expect it to not send the message to delivery-push
        verify(mockSqsSender, times(0)).pushSendEvent(any());
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId(anyString());

        // Not called because it is a PROGRESS event
        verify(mockRequestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(anyString(), any(Instant.class));
    }
}
