package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNAG012Wrapper;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaRequestId;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaStatusCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class Complex890MessageHandlerTest {

    private SqsSender sqsSender;

    private EventMetaDAO eventMetaDAO;

    private MetaDematCleaner metaDematCleaner;

    private Complex890MessageHandler handler;

    private int DAYS_REFINEMENT = 10;

    @BeforeEach
    public void init() {
        sqsSender = mock(SqsSender.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        metaDematCleaner = mock(MetaDematCleaner.class);

        when(metaDematCleaner.clean(anyString())).thenReturn(Mono.empty());

        handler = new Complex890MessageHandler(sqsSender, eventMetaDAO, metaDematCleaner, Duration.of(DAYS_REFINEMENT, ChronoUnit.DAYS));
    }

    //CASO 1.ii
    @Test
    void handleMessageMETAPNAG012PresentMETARECAG012NotPresentTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String requestId = "requestId";
        String metadataRequestid = buildMetaRequestId(requestId);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode("RECAG005C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        PnEventMeta pnEventMetaPNAG012 = new PnEventMeta();
        pnEventMetaPNAG012.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaPNAG012.setMetaStatusCode(buildMetaStatusCode("PNAG012"));

        when(eventMetaDAO.findAllByRequestId(metadataRequestid)).thenReturn(Flux.just(pnEventMetaPNAG012));

        StepVerifier.create(handler.handleMessage(entity, paperRequest))
                        .expectError(PnGenericException.class)
                                .verify();

        verify(eventMetaDAO, times(0)).createOrUpdate(any(PnEventMeta.class));

        verify(sqsSender, times(0)).pushSendEvent(any(SendEvent.class));
    }

    //CASO 2
    @Test
    void handleMessageMETAPNAG012PresentMETARECAG012PresentTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String requestId = "requestId";
        String metadataRequestid = buildMetaRequestId(requestId);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode("RECAG005C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        PnEventMeta pnEventMetaPNAG012 = new PnEventMeta();
        pnEventMetaPNAG012.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaPNAG012.setMetaStatusCode(buildMetaStatusCode("PNAG012"));

        PnEventMeta pnEventMetaRECAG012 = new PnEventMeta();
        pnEventMetaRECAG012.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG012.setMetaStatusCode(buildMetaStatusCode("RECAG012"));

        when(eventMetaDAO.findAllByRequestId(metadataRequestid)).thenReturn(Flux.just(pnEventMetaPNAG012, pnEventMetaRECAG012));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(eventMetaDAO, times(0)).createOrUpdate(any(PnEventMeta.class));

        //lo statusCode dell'entity viene modificato dall'handler
        assertThat(entity.getStatusDetail()).isEqualTo(StatusCodeEnum.PROGRESS.getValue());
        SendEvent sendEvent = SendEventMapper.createSendEventMessage(entity, paperRequest);

        verify(sqsSender, times(1)).pushSendEvent(sendEvent);
    }

    //CASO 3
    @Test
    void handleMessageMETAPNAG012NotPresentMETARECAG012PresentLessThan10DaysTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String requestId = "requestId";
        String metadataRequestid = buildMetaRequestId(requestId);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode("RECAG005C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("OK");
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        PnEventMeta pnEventMetaRECAG012 = new PnEventMeta();
        pnEventMetaRECAG012.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG012.setMetaStatusCode(buildMetaStatusCode("RECAG012"));
        pnEventMetaRECAG012.setStatusDateTime(Instant.parse("2023-03-16T17:07:00.000Z"));

        PnEventMeta pnEventMetaRECAG011A = new PnEventMeta();
        pnEventMetaRECAG011A.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG011A.setMetaStatusCode(buildMetaStatusCode("RECAG011A"));
        pnEventMetaRECAG011A.setStatusDateTime(Instant.parse("2023-03-16T17:07:00.000Z"));

        PnEventMeta pnEventMetaRECAG005A = new PnEventMeta();
        pnEventMetaRECAG005A.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG005A.setMetaStatusCode(buildMetaStatusCode("RECAG005A"));
        pnEventMetaRECAG005A.setStatusDateTime(Instant.parse("2023-03-16T17:07:00.000Z"));

        when(eventMetaDAO.findAllByRequestId(metadataRequestid)).thenReturn(Flux.just(pnEventMetaRECAG012, pnEventMetaRECAG011A, pnEventMetaRECAG005A));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        ArgumentCaptor<SendEvent> sendEventArgumentCaptor = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, times(1)).pushSendEvent(sendEventArgumentCaptor.capture());

        //verifico che viene inviato a delivery-push l'evento originale RECAG005C in stato OK
        SendEvent sendEvent = SendEventMapper.createSendEventMessage(entity, paperRequest);
        assertThat(sendEvent.getStatusCode()).isEqualTo(StatusCodeEnum.OK);
        assertThat(sendEvent.getStatusDetail()).isEqualTo("RECAG005C");
        assertThat(sendEventArgumentCaptor.getAllValues().get(0)).isEqualTo(sendEvent);
    }

    @Test
    void handleMessageMETAPNAG012NotPresentMETARECAG012PresentNotLessThan10DaysTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String requestId = "requestId";
        String metadataRequestid = buildMetaRequestId(requestId);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode("RECAG005C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("OK");
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        PnEventMeta pnEventMetaRECAG012 = new PnEventMeta();
        pnEventMetaRECAG012.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG012.setMetaStatusCode(buildMetaStatusCode("RECAG012"));
        pnEventMetaRECAG012.setStatusDateTime(Instant.parse("2023-03-16T17:07:00.000Z"));

        PnEventMeta pnEventMetaRECAG011A = new PnEventMeta();
        pnEventMetaRECAG011A.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG011A.setMetaStatusCode(buildMetaStatusCode("RECAG011A"));
        pnEventMetaRECAG011A.setStatusDateTime(Instant.parse("2023-03-05T17:07:00.000Z")); // 11 days before than RECAG005A

        PnEventMeta pnEventMetaRECAG005A = new PnEventMeta();
        pnEventMetaRECAG005A.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG005A.setMetaStatusCode(buildMetaStatusCode("RECAG005A"));
        pnEventMetaRECAG005A.setStatusDateTime(Instant.parse("2023-03-16T17:07:00.000Z"));

        when(eventMetaDAO.findAllByRequestId(metadataRequestid)).thenReturn(Flux.just(pnEventMetaRECAG012, pnEventMetaRECAG011A, pnEventMetaRECAG005A));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //verifico che viene inviato a delivery-push l'evento finale PNAG012
        PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, pnEventMetaRECAG011A.getStatusDateTime().plus(DAYS_REFINEMENT, ChronoUnit.DAYS));
        PnDeliveryRequest pnDeliveryRequestPNAG012 = pnag012Wrapper.getPnDeliveryRequestPNAG012();
        PaperProgressStatusEventDto paperProgressStatusEventDtoPNAG012 = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
        SendEvent sendPNAG012Event = SendEventMapper.createSendEventMessage(pnDeliveryRequestPNAG012, paperProgressStatusEventDtoPNAG012);
        assertThat(sendPNAG012Event.getStatusCode()).isEqualTo(StatusCodeEnum.OK);
        assertThat(sendPNAG012Event.getStatusDetail()).isEqualTo("PNAG012");
        assertThat(sendPNAG012Event.getStatusDateTime()).isEqualTo("2023-03-15T17:07:00.000Z");

        ArgumentCaptor<SendEvent> sendEventArgumentCaptor = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, times(2)).pushSendEvent(sendEventArgumentCaptor.capture());
        sendPNAG012Event.setClientRequestTimeStamp(sendEventArgumentCaptor.getAllValues().get(0).getClientRequestTimeStamp());
        assertThat(sendEventArgumentCaptor.getAllValues().get(0)).isEqualTo(sendPNAG012Event);

        //verifico che viene inviato a delivery-push l'evento originale RECAG005C in stato PROGRESS
        SendEvent sendEvent = SendEventMapper.createSendEventMessage(entity, paperRequest);
        assertThat(sendEvent.getStatusCode()).isEqualTo(StatusCodeEnum.PROGRESS);
        assertThat(sendEvent.getStatusDetail()).isEqualTo("RECAG005C");
        assertThat(sendEventArgumentCaptor.getAllValues().get(1)).isEqualTo(sendEvent);
    }
}
