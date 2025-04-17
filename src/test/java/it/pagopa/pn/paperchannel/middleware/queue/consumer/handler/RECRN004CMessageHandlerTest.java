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
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN004CMessageHandler;
import it.pagopa.pn.paperchannel.model.RequestErrorCategoryEnum;
import it.pagopa.pn.paperchannel.model.RequestErrorCauseEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RECRN004CMessageHandlerTest {
    private static final String STATUS_RECRN010 = "RECRN010";
    private static final String STATUS_RECRN004A = "RECRN004A";
    private static final String STATUS_RECRN004C = "RECRN004C";
    private static final String STATUS_PNRN012 = "PNRN012";

    private static final String requestId = "1234LL-GGGG-SSSS";
    private static final String META_STRING = "META##";
    private static final int DAYS_REFINEMENT = 10;

    @Mock
    private SqsSender sqsSender;
    @Mock
    private EventMetaDAO eventMetaDAO;
    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;
    @Mock
    private MetaDematCleaner metaDematCleaner;

    private RECRN004CMessageHandler handler;
    private PnPaperChannelConfig pnPaperChannelConfig;

    @BeforeEach
    void setUp(){
        pnPaperChannelConfig = new PnPaperChannelConfig();
        pnPaperChannelConfig.setRefinementDuration(Duration.of(DAYS_REFINEMENT, ChronoUnit.DAYS));
        pnPaperChannelConfig.setEnableTruncatedDateForRefinementCheck(true);
        pnPaperChannelConfig.setEnableOldFlowRECRN004C(false);

        handler = RECRN004CMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .build();
    }

    @Test
    void when_RECRN004AGreaterThenRECRN010of10Days_then_pushPNRN012Status() {
        // Arrange
        var now = Instant.now();
        PnEventMeta eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, now.minus(DAYS_REFINEMENT, ChronoUnit.DAYS));
        PnEventMeta eventMetaRECRN004A = getEventMeta(STATUS_RECRN004A, now);

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN010)))
                .thenReturn(Mono.just(eventMetaRECRN010));
        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN004A)))
                .thenReturn(Mono.just(eventMetaRECRN004A));
        when(metaDematCleaner.clean(requestId)).thenReturn(Mono.empty());
        doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(STATUS_RECRN004C)
                .statusDateTime(OffsetDateTime.now())
                .clientRequestTimeStamp(OffsetDateTime.now())
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(STATUS_RECRN004C);
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));
        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        // Act
        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);

        // Assert
        assertDoesNotThrow(() -> mono.block());
        verify(sqsSender, times(2)).pushSendEvent(capturedSendEvent.capture());
        assertNotNull(capturedSendEvent.getAllValues());
        assertEquals(2, capturedSendEvent.getAllValues().size());
        assertEquals(STATUS_PNRN012, capturedSendEvent.getAllValues().get(0).getStatusDetail());
        assertEquals(StatusCodeEnum.OK, capturedSendEvent.getAllValues().get(0).getStatusCode());
        assertEquals(STATUS_RECRN004C, capturedSendEvent.getAllValues().get(1).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, capturedSendEvent.getAllValues().get(1).getStatusCode());

        verify(requestDeliveryDAO, times(1))
                .updateConditionalOnFeedbackStatus(argThat(pnDeliveryRequest -> {
                    assertThat(pnDeliveryRequest).isNotNull();
                    assertThat(pnDeliveryRequest.getRefined()).isTrue();
                    assertThat(pnDeliveryRequest.getFeedbackStatusCode()).isEqualTo(STATUS_PNRN012);
                    return true;
                }), eq(true));

        verify(requestDeliveryDAO, times(1)).updateConditionalOnFeedbackStatus(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            assertThat(pnDeliveryRequest.getFeedbackStatusCode()).isEqualTo("PNRN012");
            return true;
        }), eq(true));
    }

    @Test
    void when_RECRN004ALessThanRECRN010by1Days_then_savePnEventError() {
        // Arrange
        var now = Instant.now();
        PnEventMeta eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, now);
        PnEventMeta eventMetaRECRN005A = getEventMeta(STATUS_RECRN004A, now.minus(1, ChronoUnit.DAYS));

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN010)))
                .thenReturn(Mono.just(eventMetaRECRN010));
        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN004A)))
                .thenReturn(Mono.just(eventMetaRECRN005A));

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(STATUS_RECRN004C)
                .statusDateTime(OffsetDateTime.now())
                .clientRequestTimeStamp(OffsetDateTime.now())
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(STATUS_RECRN004C);
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

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
            assertThat(error.getError()).contains("RECRN004A statusDateTime", "RECRN010 statusDateTime");
            assertThat(error.getFlowThrow()).isEqualTo("RECRN004C");
            assertThat(error.getCategory()).isEqualTo(RequestErrorCategoryEnum.RENDICONTAZIONE_SCARTATA.getValue());
            assertThat(error.getCause()).startsWith(RequestErrorCauseEnum.REFINEMENT_DATE_ERROR.getValue());
            return true;
        }));
    }

    @Test
    void when_RECRN004ALessThenRECRN010Of10Days_oldFlow_then_pushOnQueue(){
        // Arrange
        pnPaperChannelConfig.setEnableOldFlowRECRN004C(true); // Enable old flow

        var now = Instant.now();
        PnEventMeta eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, now.minus(DAYS_REFINEMENT-1, ChronoUnit.DAYS));
        PnEventMeta eventMetaRECRN003A = getEventMeta(STATUS_RECRN004A, now);

        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN010)))
                .thenReturn(Mono.just(eventMetaRECRN010));
        when(eventMetaDAO.getDeliveryEventMeta(META_STRING.concat(requestId), META_STRING.concat(STATUS_RECRN004A)))
                .thenReturn(Mono.just(eventMetaRECRN003A));
        when(metaDematCleaner.clean(requestId)).thenReturn(Mono.empty());

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(requestId)
                .statusCode(STATUS_RECRN004C)
                .statusDateTime(OffsetDateTime.now())
                .clientRequestTimeStamp(OffsetDateTime.now())
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(requestId);
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        entity.setStatusCode(ExternalChannelCodeEnum.getStatusCode(paperRequest.getStatusCode()));

        // Act
        Mono<Void> mono = this.handler.handleMessage(entity, paperRequest);

        // Assert
        Assertions.assertDoesNotThrow(() -> mono.block());
        verify(sqsSender).pushSendEvent(capturedSendEvent.capture());
        log.info(capturedSendEvent.getAllValues().toString());
        SendEvent sendEvent = capturedSendEvent.getValue();
        Assertions.assertEquals(StatusCodeEnum.PROGRESS, sendEvent.getStatusCode());
        Assertions.assertEquals(STATUS_RECRN004C, sendEvent.getStatusDetail());

        verify(requestDeliveryDAO, never()).updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean());
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
