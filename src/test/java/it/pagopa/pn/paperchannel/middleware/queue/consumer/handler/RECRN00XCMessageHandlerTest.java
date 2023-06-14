package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;


import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;


@Slf4j
class RECRN00XCMessageHandlerTest {
    private static final String statusRECRN011 = "RECRN011";
    private static final String statusRECRN003C = "RECRN003C";
    private static final String requestId = "1234LL-GGGG-SSSS";
    private static final String META_STRING = "META##";
    private RECRN00XCMessageHandler handler;
    private SqsSender sqsSender;
    private EventMetaDAO eventMetaDAO;
    private MetaDematCleaner metaDematCleaner;

    private int DAYS_REFINEMENT = 10;

    @BeforeEach
    void setUp(){
        sqsSender = mock(SqsSender.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        metaDematCleaner = mock(MetaDematCleaner.class);

        when(metaDematCleaner.clean(requestId)).thenReturn(Mono.empty());

        handler = new RECRN00XCMessageHandler(sqsSender, eventMetaDAO, metaDematCleaner, Duration.of(DAYS_REFINEMENT, ChronoUnit.DAYS));
    }

    @Test
    void whenRECRN00XAGretherThenRECRN011Of10DaysThenPushPNRN012Status(){
        String statusRECRN003A = "RECRN003A";

        PnEventMeta eventMetaRECRN011 = getEventMeta(statusRECRN011, Instant.now().minus(40, ChronoUnit.DAYS));
        PnEventMeta eventMetaRECRN003A = getEventMeta(statusRECRN003A, Instant.now().minus(20, ChronoUnit.DAYS));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN011)))
                .thenReturn(Mono.just(eventMetaRECRN011));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN003A)))
                .thenReturn(Mono.just(eventMetaRECRN003A));

        doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(statusRECRN003C)
                .statusDateTime(OffsetDateTime.now())
                .clientRequestTimeStamp(OffsetDateTime.now())
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(statusRECRN003C);
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);
        Assertions.assertDoesNotThrow(() -> mono.block());
    }


    @Test
    void whenRECRN00XALessThenRECRN011Of10DaysThenPushOnQueue(){
        String statusRECRN003A = "RECRN003A";

        PnEventMeta eventMetaRECRN011 = getEventMeta(statusRECRN011, Instant.now().minus(DAYS_REFINEMENT, ChronoUnit.DAYS));
        PnEventMeta eventMetaRECRN003A = getEventMeta(statusRECRN003A, Instant.now().minus(DAYS_REFINEMENT / 2, ChronoUnit.DAYS));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN011)))
                .thenReturn(Mono.just(eventMetaRECRN011));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN003A)))
                .thenReturn(Mono.just(eventMetaRECRN003A));

        doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(statusRECRN003C)
                .statusDateTime(OffsetDateTime.now())
                .clientRequestTimeStamp(OffsetDateTime.now())
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);
        Assertions.assertDoesNotThrow(() -> mono.block());
    }


    private PnEventMeta getEventMeta(String statusCode, Instant time){
        final int ttlOffsetDays = 365;
        final PnDiscoveredAddress address1 = new PnDiscoveredAddress();
        address1.setAddress("discoveredAddress1");
        var eventMeta = new PnEventMeta();
        eventMeta.setMetaRequestId(META_STRING.concat(requestId));
        eventMeta.setMetaStatusCode(META_STRING.concat(statusCode));
        eventMeta.setRequestId("1234");
        eventMeta.setStatusCode(statusCode);
        eventMeta.setDiscoveredAddress(address1);
        eventMeta.setDeliveryFailureCause("failureCause1");
        eventMeta.setStatusDateTime(time);
        eventMeta.setTtl(Instant.now().plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());
        return eventMeta;
    }




}
