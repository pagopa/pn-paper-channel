package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDemtaCleaner;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaRequestId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class Complex890MessageHandlerTest {

    private SqsSender sqsSender;

    private EventMetaDAO eventMetaDAO;

    private MetaDemtaCleaner metaDemtaCleaner;

    private Complex890MessageHandler handler;

    @BeforeEach
    public void init() {
        sqsSender = mock(SqsSender.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        metaDemtaCleaner = mock(MetaDemtaCleaner.class);

        when(metaDemtaCleaner.clean(anyString())).thenReturn(Mono.empty());

        handler = new Complex890MessageHandler(sqsSender, eventMetaDAO, 365L, metaDemtaCleaner);
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
}
