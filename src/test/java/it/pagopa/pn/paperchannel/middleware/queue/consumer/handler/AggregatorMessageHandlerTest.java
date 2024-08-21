package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.InvalidEventOrderException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AggregatorMessageHandlerTest {

    private AggregatorMessageHandler handler;

    private SqsSender mockSqsSender;
    private EventDematDAO mockDematDao;
    private EventMetaDAO mockMetaDao;
    private RequestDeliveryDAO requestDeliveryDAO;

    @BeforeEach
    public void init() {
        mockSqsSender = mock(SqsSender.class);
        mockMetaDao = mock(EventMetaDAO.class);
        mockDematDao = mock(EventDematDAO.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);

        PnPaperChannelConfig pnPaperChannelConfig = mock(PnPaperChannelConfig.class);
        Mockito.when(pnPaperChannelConfig.getAllowedRedriveProgressStatusCodes()).thenReturn(List.of());

        MetaDematCleaner  metaDematCleaner = new MetaDematCleaner(mockDematDao, mockMetaDao);

        handler = AggregatorMessageHandler.builder()
                .sqsSender(mockSqsSender)
                .eventMetaDAO(mockMetaDao)
                .metaDematCleaner(metaDematCleaner)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
    }

    @Test
    void handleAggregatorMessageTest() {
        // inserimento 1 meta
        PnEventMeta eventMeta = createPnEventMeta();
        // inserimento 1 demat
        PnEventDemat eventDemat = createPnEventDemat();

        // entity and paperRequest
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .statusCode("RECRS002C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        // mock: when
        // getDeliveryEventMeta
        when(mockMetaDao.getDeliveryEventMeta(any(String.class), any(String.class))).thenReturn(Mono.just(eventMeta));
        // findAllByRequestId
        when(mockMetaDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventMeta));
        when(mockDematDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventDemat));
        // the batch deletes
        when(mockMetaDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.empty());
        when(mockDematDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.empty());

        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));
        // assertDoNotThrow with call
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // check invocations: verify
        // getDeliveryEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).getDeliveryEventMeta(any(String.class), any(String.class));
        // deleteEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).deleteBatch(any(String.class), any(String.class));
        // deleteEventDemat call
        verify(mockDematDao, timeout(2000).times(1)).deleteBatch(any(String.class), any(String.class));

        verify(requestDeliveryDAO, timeout(2000).times(1)).updateConditionalOnFeedbackStatus(entity, true);

        // DeliveryPush send via SQS verification
        verify(mockSqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        // arricchimento
        assertEquals("failureCause1", caturedSendEvent.getValue().getDeliveryFailureCause());
        assertNotNull(caturedSendEvent.getValue().getDiscoveredAddress());
        assertEquals("discoveredAddress1", caturedSendEvent.getValue().getDiscoveredAddress().getAddress());
    }

    @Test
    void handleAggregatorMessageMissingMetaTest() {
        // inserimento 1 demat
        PnEventDemat eventDemat = createPnEventDemat();

        // entity and paperRequest
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .statusCode("RECRS002C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        // mock: when
        // getDeliveryEventMeta
        when(mockMetaDao.getDeliveryEventMeta(any(String.class), any(String.class))).thenReturn(Mono.empty());
        // findAllByRequestId
        when(mockMetaDao.findAllByRequestId(any(String.class))).thenReturn(Flux.empty());
        when(mockDematDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventDemat));

        // deleteEventMeta
        when(mockMetaDao.deleteEventMeta(any(String.class), any(String.class))).thenReturn(Mono.empty());

        // assertDoNotThrow with call
        assertThrows(InvalidEventOrderException.class, () -> handler.handleMessage(entity, paperRequest).block());

        // check invocations: verify
        // getDeliveryEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).getDeliveryEventMeta(any(String.class), any(String.class));

        // DeliveryPush send via SQS verification
        verify(mockSqsSender, timeout(2000).times(0)).pushSendEvent(any());

        verify(requestDeliveryDAO, never()).updateData(entity);
    }

    @Test
    void handleAggregatorMessageSQSFailTest() {
        // inserimento 1 meta
        PnEventMeta eventMeta = createPnEventMeta();
        // inserimento 1 demat
        PnEventDemat eventDemat = createPnEventDemat();

        // entity and paperRequest
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .statusCode("RECRS002C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        // mock: when
        // getDeliveryEventMeta
        when(mockMetaDao.getDeliveryEventMeta(any(String.class), any(String.class))).thenReturn(Mono.just(eventMeta));
        // findAllByRequestId
        when(mockMetaDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventMeta));
        when(mockDematDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventDemat));
        // the batch deletes
        when(mockMetaDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.empty());
        when(mockDematDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.empty());

        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // the SQS queue
        doThrow(new RuntimeException()).when(mockSqsSender).pushSendEvent(Mockito.any());

        // assertThrows with call
        assertThrowsExactly(RuntimeException.class, () -> handler.handleMessage(entity, paperRequest).block());

        // check invocations: verify
        // getDeliveryEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).getDeliveryEventMeta(any(String.class), any(String.class));
        // DeliveryPush send via SQS verification
        verify(mockSqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));
        // deleteEventMeta call
        verify(mockMetaDao, timeout(2000).times(0)).deleteBatch(any(String.class), any(String.class));
        // deleteEventDemat call
        verify(mockDematDao, timeout(2000).times(0)).deleteBatch(any(String.class), any(String.class));

        // No update because of sqs failure
        verify(requestDeliveryDAO, timeout(2000).times(0)).updateData(any(PnDeliveryRequest.class), anyBoolean());
    }

    @Test
    void handleAggregatorMessageDeleteMetaFailTest() {
        // inserimento 1 meta
        PnEventMeta eventMeta = createPnEventMeta();
        // inserimento 1 demat
        PnEventDemat eventDemat = createPnEventDemat();

        // entity and paperRequest
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .statusCode("RECRS002C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        // mock: when
        // getDeliveryEventMeta
        when(mockMetaDao.getDeliveryEventMeta(any(String.class), any(String.class))).thenReturn(Mono.just(eventMeta));
        // findAllByRequestId
        when(mockMetaDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventMeta));
        when(mockDematDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventDemat));
        // the batch deletes
        when(mockMetaDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.error(new RuntimeException()));
        when(mockDematDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.empty());

        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // assertDoNotThrow with call
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // check invocations: verify
        // getDeliveryEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).getDeliveryEventMeta(any(String.class), any(String.class));
        // deleteEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).deleteBatch(any(String.class), any(String.class));
        // deleteEventDemat call
        verify(mockDematDao, timeout(2000).times(1)).deleteBatch(any(String.class), any(String.class));
        // DeliveryPush send via SQS verification
        verify(mockSqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));

        verify(requestDeliveryDAO, timeout(2000).times(1)).updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean());
    }

    @Test
    void handleAggregatorMessageDeleteDematFailTest() {
        // inserimento 1 meta
        PnEventMeta eventMeta = createPnEventMeta();
        // inserimento 1 demat
        PnEventDemat eventDemat = createPnEventDemat();

        // entity and paperRequest
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0")
                .statusCode("RECRS002C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        // mock: when
        // getDeliveryEventMeta
        when(mockMetaDao.getDeliveryEventMeta(any(String.class), any(String.class))).thenReturn(Mono.just(eventMeta));
        // findAllByRequestId
        when(mockMetaDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventMeta));
        when(mockDematDao.findAllByRequestId(any(String.class))).thenReturn(Flux.just(eventDemat));
        // the batch deletes
        when(mockMetaDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.empty());
        when(mockDematDao.deleteBatch(any(String.class), any(String.class))).thenReturn(Mono.error(new RuntimeException()));

        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // assertDoNotThrow with call
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // check invocations: verify
        // getDeliveryEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).getDeliveryEventMeta(any(String.class), any(String.class));
        // deleteEventMeta call
        verify(mockMetaDao, timeout(2000).times(1)).deleteBatch(any(String.class), any(String.class));
        // deleteEventDemat call
        verify(mockDematDao, timeout(2000).times(1)).deleteBatch(any(String.class), any(String.class));
        // DeliveryPush send via SQS verification
        verify(mockSqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));

        verify(requestDeliveryDAO, timeout(2000).times(1)).updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean());
    }

    private PnEventMeta createPnEventMeta() {
        int ttlOffsetDays = 365;
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0";
        String statusCode = "RECRS002A";

        PnEventMeta eventMeta = new PnEventMeta();
        PnDiscoveredAddress address1 = new PnDiscoveredAddress();
        address1.setAddress("discoveredAddress1");
        eventMeta.setMetaRequestId("META##" + requestId);
        eventMeta.setMetaStatusCode("META##" + statusCode);
        eventMeta.setRequestId(requestId);
        eventMeta.setStatusCode(statusCode);
        eventMeta.setDiscoveredAddress(address1);
        eventMeta.setDeliveryFailureCause("failureCause1");
        eventMeta.setStatusDateTime(Instant.now());
        eventMeta.setTtl(Instant.now().plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        return eventMeta;
    }

    private PnEventDemat createPnEventDemat() {
        int ttlOffsetDays = 365;
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0";
        String statusCode = "RECRS002A";
        String productType = "RS";

        PnEventDemat eventDemat = new PnEventDemat();
        eventDemat.setDematRequestId("DEMAT##" + requestId);
        eventDemat.setDocumentTypeStatusCode(productType + "##" + statusCode);
        eventDemat.setRequestId(requestId);
        eventDemat.setDocumentType(productType);
        eventDemat.setStatusCode(statusCode);
        eventDemat.setDocumentDate(Instant.now().plusSeconds(-5));
        eventDemat.setStatusDateTime(Instant.now());
        eventDemat.setUri("uri1");
        eventDemat.setTtl(Instant.now().plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        return eventDemat;
    }
}
