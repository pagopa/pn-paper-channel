package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
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

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class Complex890MessageHandlerTest {

    private SqsSender sqsSender;

    private EventMetaDAO eventMetaDAO;

    private MetaDematCleaner metaDematCleaner;

    private Complex890MessageHandler handler;

    @BeforeEach
    public void init() {
        sqsSender = mock(SqsSender.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        metaDematCleaner = mock(MetaDematCleaner.class);

        when(metaDematCleaner.clean(anyString())).thenReturn(Mono.empty());

        handler = new Complex890MessageHandler(sqsSender, eventMetaDAO, metaDematCleaner);
    }


    //CASO 4
    @Test
    void handleMessageCollectionEmptyTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String requestId = "requestId";
        String metadataRequestid = buildMetaRequestId(requestId);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode("RECRS002A")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

        when(eventMetaDAO.findAllByRequestId(metadataRequestid)).thenReturn(Flux.empty());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(eventMetaDAO, times(0)).createOrUpdate(any(PnEventMeta.class));

        //lo statusCode dell'entity viene modificato dall'handler
        assertThat(entity.getStatusCode()).isEqualTo(StatusCodeEnum.OK.getValue());
        SendEvent sendEvent = new SaveDematMessageHandler(null, null, null).createSendEventMessage(entity, paperRequest);

        verify(sqsSender, times(1)).pushSendEvent(sendEvent);
    }

    //CASO 1.ii
    @Test
    void handleMessageMETAPNAG012PresentMETARECAG012NotPresentTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String requestId = "requestId";
        String metadataRequestid = buildMetaRequestId(requestId);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode("RECRS002A")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

        PnEventMeta pnEventMetaPNAG012 = new PnEventMeta();
        pnEventMetaPNAG012.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaPNAG012.setMetaStatusCode(buildMetaStatusCode("PNAG012"));

        when(eventMetaDAO.findAllByRequestId(metadataRequestid)).thenReturn(Flux.just(pnEventMetaPNAG012));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

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
                .statusCode("RECRS002A")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

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
        assertThat(entity.getStatusCode()).isEqualTo(StatusCodeEnum.PROGRESS.getValue());
        SendEvent sendEvent = new SaveDematMessageHandler(null, null, null).createSendEventMessage(entity, paperRequest);

        verify(sqsSender, times(1)).pushSendEvent(sendEvent);
    }

    //CASO 3
    @Test
    void handleMessageMETAPNAG012NotPresentMETARECAG012PresentTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String requestId = "requestId";
        String metadataRequestid = buildMetaRequestId(requestId);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode("RECRS002A")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

        PnEventMeta pnEventMetaRECAG012 = new PnEventMeta();
        pnEventMetaRECAG012.setMetaRequestId(buildMetaRequestId(requestId));
        pnEventMetaRECAG012.setMetaStatusCode(buildMetaStatusCode("RECAG012"));
        pnEventMetaRECAG012.setStatusDateTime(Instant.parse("2023-03-16T17:07:00.000Z"));

        when(eventMetaDAO.findAllByRequestId(metadataRequestid)).thenReturn(Flux.just(pnEventMetaRECAG012));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //lo statusCode dell'entity e lo statusDateTime di paperRequest vengono modificati dall'handler
        assertThat(entity.getStatusCode()).isEqualTo(StatusCodeEnum.OK.getValue());
        assertThat(entity.getStatusDetail()).isEqualTo("Distacco d'ufficio 23L fascicolo chiuso");
        assertThat(paperRequest.getStatusDateTime().toInstant()).isEqualTo(pnEventMetaRECAG012.getStatusDateTime());

        SendEvent sendEvent = new SaveDematMessageHandler(null, null, null).createSendEventMessage(entity, paperRequest);

        verify(sqsSender, times(1)).pushSendEvent(sendEvent);
    }
}
