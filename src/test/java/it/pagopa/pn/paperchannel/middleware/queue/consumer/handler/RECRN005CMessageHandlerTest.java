package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;


import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN005CMessageHandler;
import it.pagopa.pn.paperchannel.model.RequestErrorCategoryEnum;
import it.pagopa.pn.paperchannel.model.RequestErrorCauseEnum;
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
class RECRN005CMessageHandlerTest {
    private static final String STATUS_RECRN011 = "RECRN011";
    private static final String STATUS_RECRN005A = "RECRN005A";
    private static final String STATUS_RECRN005C = "RECRN005C";
    private static final String STATUS_PNRN012 = "PNRN012";

    private static final String requestId = "1234LL-GGGG-SSSS";
    private static final String META_STRING = "META##";

    private RECRN005CMessageHandler handler;

    private SqsSender sqsSender;
    private EventMetaDAO eventMetaDAO;
    private RequestDeliveryDAO requestDeliveryDAO;
    private PaperRequestErrorDAO paperRequestErrorDAO;

    private final int DAYS_REFINEMENT = 10;
    private final int STORAGE_DURATION_AR_DAYS = 30;

    @BeforeEach
    void setUp(){
        sqsSender = mock(SqsSender.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);
        paperRequestErrorDAO = mock(PaperRequestErrorDAO.class);

        MetaDematCleaner metaDematCleaner = mock(MetaDematCleaner.class);

        when(metaDematCleaner.clean(requestId)).thenReturn(Mono.empty());

        PnPaperChannelConfig mockConfig = new PnPaperChannelConfig();
        mockConfig.setRefinementDuration(Duration.of(DAYS_REFINEMENT, ChronoUnit.DAYS));

        handler = RECRN005CMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(mockConfig)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .build();
    }

    @Test
    void shouldPush_PNRN012_when_RECRN005_greaterOrEquals_RECRN011_by30Days (){
        // Arrange
        var now = Instant.now();
        PnEventMeta eventMetaRECRN011 = getEventMeta(STATUS_RECRN011,now.minus(STORAGE_DURATION_AR_DAYS, ChronoUnit.DAYS));
        PnEventMeta eventMetaRECRN005A = getEventMeta(STATUS_RECRN005A, now);

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN011)))
                .thenReturn(Mono.just(eventMetaRECRN011));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN005A)))
                .thenReturn(Mono.just(eventMetaRECRN005A));

        doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(STATUS_RECRN005C)
                .statusDateTime(OffsetDateTime.now())
                .clientRequestTimeStamp(OffsetDateTime.now());

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(STATUS_RECRN005C);
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean()))
                .thenReturn(Mono.just(entity));
        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        // Act
        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);
        Assertions.assertDoesNotThrow(() -> mono.block());

        // Assert
        verify(sqsSender, times(2)).pushSendEvent(capturedSendEvent.capture());
        assertNotNull(capturedSendEvent.getAllValues());
        assertEquals(2, capturedSendEvent.getAllValues().size());
        assertEquals("PNRN012", capturedSendEvent.getAllValues().get(0).getStatusDetail());
        assertEquals(StatusCodeEnum.OK, capturedSendEvent.getAllValues().get(0).getStatusCode());

        verify(requestDeliveryDAO, times(1))
                .updateConditionalOnFeedbackStatus(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            assertThat(pnDeliveryRequest.getFeedbackStatusCode()).isEqualTo(STATUS_PNRN012);
            return true;
        }), eq(true));
    }

    @Test
    void shouldSavePnEventError_when_RECRN005_less_RECRN011_by1Days() {
        // Arrange
        var now = Instant.now();
        PnEventMeta eventMetaRECRN011 = getEventMeta(STATUS_RECRN011, now);
        PnEventMeta eventMetaRECRN005A = getEventMeta(STATUS_RECRN005A, now.minus(1, ChronoUnit.DAYS));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN011)))
                .thenReturn(Mono.just(eventMetaRECRN011));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN005A)))
                .thenReturn(Mono.just(eventMetaRECRN005A));

        doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(STATUS_RECRN005C)
                .statusDateTime(OffsetDateTime.now())
                .clientRequestTimeStamp(OffsetDateTime.now())
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(STATUS_RECRN005C);
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean()))
                .thenReturn(Mono.just(entity));
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(new PnRequestError()));

        // Act
        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);
        Assertions.assertDoesNotThrow(() -> mono.block());

        // Assert
        verify(sqsSender, never()).pushSendEvent(any());
        verify(requestDeliveryDAO, never()).updateConditionalOnFeedbackStatus(any(), anyBoolean());

        // Verify requestError
        verify(paperRequestErrorDAO, times(1)).created(argThat(error -> {
            assertThat(error).isNotNull();
            assertThat(error.getRequestId()).isEqualTo(requestId);
            assertThat(error.getError()).contains("RECRN005A statusDateTime", "RECRN011 statusDateTime");
            assertThat(error.getFlowThrow()).isEqualTo("RECRN005C");
            assertThat(error.getCategory()).isEqualTo(RequestErrorCategoryEnum.RENDICONTAZIONE_SCARTATA.getValue());
            assertThat(error.getCause()).startsWith(RequestErrorCauseEnum.GIACENZA_DATE_ERROR.getValue());
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
