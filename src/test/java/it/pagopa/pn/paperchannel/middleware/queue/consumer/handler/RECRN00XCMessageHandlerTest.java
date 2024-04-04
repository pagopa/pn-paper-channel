package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;


import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private RequestDeliveryDAO requestDeliveryDAO;

    private final int DAYS_REFINEMENT = 10;

    @BeforeEach
    void setUp(){
        sqsSender = mock(SqsSender.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);

        MetaDematCleaner metaDematCleaner = mock(MetaDematCleaner.class);

        when(metaDematCleaner.clean(requestId)).thenReturn(Mono.empty());

        PnPaperChannelConfig mockConfig = new PnPaperChannelConfig();
        mockConfig.setRefinementDuration(Duration.of(DAYS_REFINEMENT, ChronoUnit.DAYS));

        handler = RECRN00XCMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(mockConfig)
                .build();
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

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(entity));

        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);
        Assertions.assertDoesNotThrow(() -> mono.block());

        verify(requestDeliveryDAO, times(1)).updateData(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            return true;
        }));
    }


    @Test
    void whenRECRN00XALessThenRECRN011Of10DaysThenPushOnQueue(){
        String statusRECRN003A = "RECRN003A";
        String statusRECRN003C = "RECRN003C";

        PnEventMeta eventMetaRECRN011 = getEventMeta(statusRECRN011, Instant.now().minus(DAYS_REFINEMENT, ChronoUnit.DAYS));
        PnEventMeta eventMetaRECRN003A = getEventMeta(statusRECRN003A, Instant.now().minus(DAYS_REFINEMENT / 2, ChronoUnit.DAYS));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN011)))
                .thenReturn(Mono.just(eventMetaRECRN011));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN003A)))
                .thenReturn(Mono.just(eventMetaRECRN003A));

        //doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);



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

        verify(sqsSender).pushSendEvent(caturedSendEvent.capture());
        SendEvent sendEvent = caturedSendEvent.getValue();
        Assertions.assertEquals(StatusCodeEnum.PROGRESS, sendEvent.getStatusCode());
        Assertions.assertEquals(statusRECRN003C, sendEvent.getStatusDetail());

        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }


    @Test
    void whenRECRN00XAMoreThenRECRN011Of10DaysThenPushOnQueue(){
        String statusRECRN003A = "RECRN003A";
        String statusRECRN003C = "RECRN003C";

        PnEventMeta eventMetaRECRN011 = getEventMeta(statusRECRN011, Instant.now().minus(DAYS_REFINEMENT * 2, ChronoUnit.DAYS));
        PnEventMeta eventMetaRECRN003A = getEventMeta(statusRECRN003A, Instant.now().minus(0, ChronoUnit.DAYS));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN011)))
                .thenReturn(Mono.just(eventMetaRECRN011));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(statusRECRN003A)))
                .thenReturn(Mono.just(eventMetaRECRN003A));

        //doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

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

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(entity));

        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);
        Assertions.assertDoesNotThrow(() -> mono.block());

        verify(sqsSender, times(2)).pushSendEvent(caturedSendEvent.capture());
        assertNotNull(caturedSendEvent.getAllValues());
        assertEquals(2, caturedSendEvent.getAllValues().size());

        assertEquals("PNRN012", caturedSendEvent.getAllValues().get(0).getStatusDetail());
        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getAllValues().get(0).getStatusCode());
        assertEquals("RECRN003C", caturedSendEvent.getAllValues().get(1).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getAllValues().get(1).getStatusCode());

        verify(requestDeliveryDAO, times(1)).updateData(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            return true;
        }));
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
