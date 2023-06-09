package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.RECRN011_STATUS_CODE;
import static org.mockito.Mockito.*;

class RECRN011MessageHandlerTest {
    private static final String statusRECRN011 = "RECRN011";
    private static final String requestId = "1234LL-GGGG-SSSS";
    private static final String META_STRING = "META##";
    private RECRN011MessageHandler messageHandler;

    private SqsSender sqsSender;
    private EventMetaDAO eventMetaDAO;

    @BeforeEach
    void setUp(){
        this.sqsSender = mock(SqsSender.class);
        this.eventMetaDAO = mock(EventMetaDAO.class);

        this.messageHandler = new RECRN011MessageHandler(sqsSender, eventMetaDAO, 14L);
    }


    @Test
    void whenStatusIsRECRN011ThenSaveAndPushOnDeliveryQueue(){
        OffsetDateTime nowTime = OffsetDateTime.now();
        PnEventMeta eventMetaSaved = getEventMeta(nowTime);

        Mockito.when(eventMetaDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(eventMetaSaved));

        doNothing().when(sqsSender).pushSendEvent(any());



        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(statusRECRN011)
                .statusDateTime(nowTime)
                .clientRequestTimeStamp(nowTime)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        Assertions.assertDoesNotThrow(() -> this.messageHandler.handleMessage(entity, paperRequest));
        verify(eventMetaDAO, times(1)).createOrUpdate(eventMetaSaved);

    }

    PnEventMeta getEventMeta(OffsetDateTime statusDateTime) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(META_STRING.concat(requestId));
        pnEventMeta.setMetaStatusCode(META_STRING.concat(RECRN011_STATUS_CODE));
        pnEventMeta.setTtl(statusDateTime.plusDays(14).toEpochSecond());

        pnEventMeta.setRequestId(requestId);
        pnEventMeta.setStatusCode(RECRN011_STATUS_CODE);
        pnEventMeta.setDeliveryFailureCause("M02");


        pnEventMeta.setStatusDateTime(statusDateTime.toInstant());
        return pnEventMeta;
    }




}
