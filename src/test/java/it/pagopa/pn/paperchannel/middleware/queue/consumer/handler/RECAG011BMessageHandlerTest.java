package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNAG012Wrapper;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.PNAG012MessageHandler.DEMAT_SORT_KEYS_FILTER;
import static it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.PNAG012MessageHandler.META_SORT_KEY_FILTER;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.createMETAForPNAG012Event;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

// Questa classe di test, testa sia l'handler RECAG011BMessageHandler che PNAG012MessageHandler
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

        PNAG012MessageHandler pnag012MessageHandler = new PNAG012MessageHandler(mockSqsSender, eventDematDAO, ttlDays, eventMetaDAO, ttlDays);
        handler = new RECAG011BMessageHandler(mockSqsSender, eventDematDAO, ttlDays, pnag012MessageHandler);
    }

    @Test
    void handleMessageOKAndInsertPNAG012Test() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG011B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        // mock per flusso normale DEMAT
        PnEventDemat pnEventDematExpected = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent eventDematToDeliveryPushExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

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

        when(eventMetaDAO.putIfAbsent(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        testNormalDematFlow(pnEventDematExpected, eventDematToDeliveryPushExpected);

        verify(eventDematDAO, times(1)).findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER);
        verify(eventMetaDAO, times(1)).getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER);
        verify(eventMetaDAO, times(1)).putIfAbsent(pnEventMeta);

        PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, eventMetaRECAG012Expected.getStatusDateTime());
        PnDeliveryRequest pnDeliveryRequestPNAG012 = pnag012Wrapper.getPnDeliveryRequestPNAG012();
        PaperProgressStatusEventDto paperProgressStatusEventDtoPNAG012 = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
        SendEvent sendPNAG012Event = SendEventMapper.createSendEventMessage(pnDeliveryRequestPNAG012, paperProgressStatusEventDtoPNAG012);

        //mi aspetto che mandi l'evento prima l'evento RECAG011B e poi il PNAG012 a delivery-push
        ArgumentCaptor<SendEvent> sendEventArgumentCaptor = ArgumentCaptor.forClass(SendEvent.class);
        verify(mockSqsSender, times(2)).pushSendEvent(sendEventArgumentCaptor.capture());

        assertThat(sendEventArgumentCaptor.getAllValues().get(0).getStatusDetail()).isEqualTo("RECAG011B");
        sendPNAG012Event.setClientRequestTimeStamp(sendEventArgumentCaptor.getAllValues().get(1).getClientRequestTimeStamp());
        assertThat(sendEventArgumentCaptor.getAllValues().get(1)).isEqualTo(sendPNAG012Event);


    }

    @Test
    void handleMessagePNAG012BNotInsertedBecauseFindAllByKeysFilterKOTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG011B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        // mock per flusso normale DEMAT
        PnEventDemat pnEventDematExpected = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent eventDematToDeliveryPushExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

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

        PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, eventMetaRECAG012Expected.getStatusDateTime());
        PnDeliveryRequest pnDeliveryRequestPNAG012 = pnag012Wrapper.getPnDeliveryRequestPNAG012();
        PaperProgressStatusEventDto paperProgressStatusEventDtoPNAG012 = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
        SendEvent sendPNAG012Event = SendEventMapper.createSendEventMessage(pnDeliveryRequestPNAG012, paperProgressStatusEventDtoPNAG012);

        //mi aspetto che NON mandi il messaggio PNAG012 a delivery-push
        verify(mockSqsSender, times(0)).pushSendEvent(sendPNAG012Event);


    }

    @Test
    void handleMessagePNAGO12BlockBecauseZeroResultGetDeliveryEventMetaTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG011B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        // mock per flusso normale DEMAT
        PnEventDemat pnEventDematExpected = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent eventDematToDeliveryPushExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

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

        PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, eventMetaRECAG012Expected.getStatusDateTime());
        PnDeliveryRequest pnDeliveryRequestPNAG012 = pnag012Wrapper.getPnDeliveryRequestPNAG012();
        PaperProgressStatusEventDto paperProgressStatusEventDtoPNAG012 = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
        SendEvent sendPNAG012Event = SendEventMapper.createSendEventMessage(pnDeliveryRequestPNAG012, paperProgressStatusEventDtoPNAG012);

        //mi aspetto che NON mandi il messaggio PNAG012 a delivery-push
        verify(mockSqsSender, times(0)).pushSendEvent(sendPNAG012Event);


    }

    @Test
    void handleMessagePNAGO12NotSendToDeliveryPushBecauseAlreadyExistsTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG011B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        // mock per flusso normale DEMAT
        PnEventDemat pnEventDematExpected = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent eventDematToDeliveryPushExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

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

        when(eventMetaDAO.putIfAbsent(pnEventMeta)).thenReturn(Mono.empty());

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        testNormalDematFlow(pnEventDematExpected, eventDematToDeliveryPushExpected);

        verify(eventDematDAO, times(1)).findAllByKeys("DEMAT##requestId", DEMAT_SORT_KEYS_FILTER);
        verify(eventMetaDAO, times(1)).getDeliveryEventMeta("META##requestId", META_SORT_KEY_FILTER);
        verify(eventMetaDAO, times(1)).putIfAbsent(pnEventMeta);

        //mi aspetto che mandi SOLO l'evento RECAG011B e non mandi l'evento PNAG012 perché non supera la putIfAbsent
        ArgumentCaptor<SendEvent> sendEventArgumentCaptor = ArgumentCaptor.forClass(SendEvent.class);
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventArgumentCaptor.capture());
        assertThat(sendEventArgumentCaptor.getAllValues().get(0).getStatusDetail()).isEqualTo("RECAG011B");


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
