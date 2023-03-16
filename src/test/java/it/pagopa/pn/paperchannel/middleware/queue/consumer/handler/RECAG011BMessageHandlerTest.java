package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECAG011BMessageHandler.DEMAT_SORT_KEYS_FILTER;
import static it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECAG011BMessageHandler.META_SORT_KEY_FILTER;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.createMETAForPNAG012Event;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.editPnDeliveryRequestForPNAG012;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class RECAG011BMessageHandlerTest {

    private EventDematDAO eventDematDAO;

    private EventMetaDAO eventMetaDAO;

    private SqsSender mockSqsSender;

    private RECAG011BMessageHandler handler;

    @BeforeEach
    public void init() {
        long ttlDays = 365;
        eventDematDAO = mock(EventDematDAO.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        mockSqsSender = mock(SqsSender.class);

        handler = new RECAG011BMessageHandler(mockSqsSender, eventDematDAO, ttlDays, eventMetaDAO, ttlDays);
    }

    @Test
    void handleMessageOKTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECRS002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .url("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

        // mock per flusso normale DEMAT
        PnEventDemat pnEventDematExpected = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent eventDematToDeliveryPushExpected = handler.createSendEventMessage(entity, paperRequest);

        when(eventDematDAO.createOrUpdate(pnEventDematExpected)).thenReturn(Mono.just(pnEventDematExpected));

        // mock per flusso PNAG012
        PnEventMeta eventMetaRECAG012Expected = createEventMetaRECAG012Expected(paperRequest);
        PnEventMeta pnEventMeta = createMETAForPNAG012Event(paperRequest, eventMetaRECAG012Expected, 365L);

        PnEventDemat pnEventDemat23L = new PnEventDemat();
        pnEventDemat23L.setDocumentTypeStatusCode("23L##RECAG011B");
        PnEventDemat pnEventDematARCAD = new PnEventDemat();
        pnEventDematARCAD.setDocumentTypeStatusCode("ARCAD##RECAG011B");

        when(eventDematDAO.findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER)).thenReturn(
                Flux.fromIterable(List.of(pnEventDemat23L, pnEventDematARCAD)));

        when(eventMetaDAO.getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(eventMetaRECAG012Expected));

        when(eventMetaDAO.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        testNormalDematFlow(pnEventDematExpected, eventDematToDeliveryPushExpected);

        verify(eventDematDAO, times(1)).findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER);
        verify(eventMetaDAO, times(1)).getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER);
        verify(eventMetaDAO, times(1)).createOrUpdate(pnEventMeta);

        editPnDeliveryRequestForPNAG012(entity);
        SendEvent sendPNAG012Event = handler.createSendEventMessage(entity, paperRequest);

        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendPNAG012Event);


    }

    @Test
    void handleMessageKOBecausefindAllByKeysFilterTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECRS002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .url("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

        // mock per flusso normale DEMAT
        PnEventDemat pnEventDematExpected = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent eventDematToDeliveryPushExpected = handler.createSendEventMessage(entity, paperRequest);

        when(eventDematDAO.createOrUpdate(pnEventDematExpected)).thenReturn(Mono.just(pnEventDematExpected));

        // mock per flusso PNAG012
        PnEventMeta eventMetaRECAG012Expected = createEventMetaRECAG012Expected(paperRequest);
        PnEventMeta pnEventMeta = createMETAForPNAG012Event(paperRequest, eventMetaRECAG012Expected, 365L);

        PnEventDemat pnEventDemat23L = new PnEventDemat();
        pnEventDemat23L.setDocumentTypeStatusCode("23L##RECAG011B");
        //viene trovato solo 23L##RECAG011B, che non basta a soddisfare il filtro

        when(eventDematDAO.findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER)).thenReturn(
                Flux.fromIterable(List.of(pnEventDemat23L)));

        when(eventMetaDAO.getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(eventMetaRECAG012Expected));

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        testNormalDematFlow(pnEventDematExpected, eventDematToDeliveryPushExpected);

        verify(eventDematDAO, times(1)).findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER);
        verify(eventMetaDAO, times(0)).getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER);
        verify(eventMetaDAO, times(0)).createOrUpdate(pnEventMeta);

        editPnDeliveryRequestForPNAG012(entity);
        SendEvent sendPNAG012Event = handler.createSendEventMessage(entity, paperRequest);

        //mi aspetto che NON mandi il messaggio PNAG012 a delivery-push
        verify(mockSqsSender, times(0)).pushSendEvent(sendPNAG012Event);


    }

    @Test
    void handleMessageKOBecauseZeroResultGetDeliveryEventMetaTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECRS002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .url("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

        // mock per flusso normale DEMAT
        PnEventDemat pnEventDematExpected = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent eventDematToDeliveryPushExpected = handler.createSendEventMessage(entity, paperRequest);

        when(eventDematDAO.createOrUpdate(pnEventDematExpected)).thenReturn(Mono.just(pnEventDematExpected));

        // mock per flusso PNAG012
        PnEventMeta eventMetaRECAG012Expected = createEventMetaRECAG012Expected(paperRequest);
        PnEventMeta pnEventMeta = createMETAForPNAG012Event(paperRequest, eventMetaRECAG012Expected, 365L);

        PnEventDemat pnEventDemat23L = new PnEventDemat();
        pnEventDemat23L.setDocumentTypeStatusCode("23L##RECAG011B");
        PnEventDemat pnEventDematARCAD = new PnEventDemat();
        pnEventDematARCAD.setDocumentTypeStatusCode("ARCAD##RECAG011B");

        when(eventDematDAO.findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER)).thenReturn(
                Flux.fromIterable(List.of(pnEventDemat23L, pnEventDematARCAD)));

        when(eventMetaDAO.getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER))
                .thenReturn(Mono.empty()); // 0 risultati

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        testNormalDematFlow(pnEventDematExpected, eventDematToDeliveryPushExpected);

        verify(eventDematDAO, times(1)).findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER);
        verify(eventMetaDAO, times(1)).getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER);
        verify(eventMetaDAO, times(0)).createOrUpdate(pnEventMeta);

        editPnDeliveryRequestForPNAG012(entity);
        SendEvent sendPNAG012Event = handler.createSendEventMessage(entity, paperRequest);

        //mi aspetto che NON mandi il messaggio PNAG012 a delivery-push
        verify(mockSqsSender, times(0)).pushSendEvent(sendPNAG012Event);


    }


    private void testNormalDematFlow(PnEventDemat pnEventDematExpected, SendEvent eventDematToDeliveryPushExpected) {

        //mi aspetto che salvi l'evento
        verify(eventDematDAO, times(1)).createOrUpdate(pnEventDematExpected);
        //mi aspetto che mandi il messaggio a delivery-push perché il product è Plico
        verify(mockSqsSender, times(1)).pushSendEvent(eventDematToDeliveryPushExpected);
    }

    private PnEventMeta createEventMetaRECAG012Expected(PaperProgressStatusEventDto paperRequest) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setTtl(365L);
        pnEventMeta.setMetaRequestId("META##" + paperRequest.getRequestId());
        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setMetaStatusCode("META##RECAG012");
        pnEventMeta.setStatusCode("RECAG012");
        pnEventMeta.setStatusDateTime(Instant.parse("2023-03-14T16:48:00.000Z"));

        return pnEventMeta;
    }


}