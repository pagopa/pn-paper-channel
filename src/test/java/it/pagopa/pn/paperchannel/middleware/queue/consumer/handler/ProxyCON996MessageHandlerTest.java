package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.impl.RequestDeliveryDAOImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProxyCON996MessageHandlerTest {

    private RetryableErrorMessageHandler retryableErrorMessageHandler;
    private NotRetryableErrorMessageHandler notRetryableErrorMessageHandler;
    private ProxyCON996MessageHandler proxyCON996MessageHandler;
    private RequestDeliveryDAOImpl dao;

    @BeforeEach
    public void init() {
        retryableErrorMessageHandler = mock(RetryableErrorMessageHandler.class);
        notRetryableErrorMessageHandler = mock(NotRetryableErrorMessageHandler.class);
        dao = mock(RequestDeliveryDAOImpl.class);

        proxyCON996MessageHandler = ProxyCON996MessageHandler.builder()
                .retryableErrorMessageHandler(retryableErrorMessageHandler)
                .notRetryableErrorMessageHandler(notRetryableErrorMessageHandler)
                .requestDeliveryDAO(dao)
                .build();
    }

    @Test
    void testHandleMessageWithApplyRasterizationTrue() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setApplyRasterization(true);
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();

        when(notRetryableErrorMessageHandler.handleMessage(any(PnDeliveryRequest.class), any(PaperProgressStatusEventDto.class)))
                .thenReturn(Mono.empty());

        Mono<Void> result = proxyCON996MessageHandler.handleMessage(entity, paperRequest);

        StepVerifier.create(result)
                .verifyComplete();

        verify(dao, never()).updateApplyRasterization(any(), any());
        verify(notRetryableErrorMessageHandler, times(1)).handleMessage(entity, paperRequest);
        verify(retryableErrorMessageHandler, never()).handleMessage(any(PnDeliveryRequest.class), any(PaperProgressStatusEventDto.class));
    }

    @Test
    void testHandleMessageWithApplyRasterizationFalse() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setApplyRasterization(false);

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();

        when(dao.updateApplyRasterization("requestId", true))
                .thenReturn(Mono.just(mock(UpdateItemResponse.class)));

        ArgumentCaptor<PnDeliveryRequest> captor = ArgumentCaptor.forClass(PnDeliveryRequest.class);
        when(retryableErrorMessageHandler.handleMessage(captor.capture(), any()))
                .thenReturn(Mono.empty());

        Mono<Void> result = proxyCON996MessageHandler.handleMessage(entity, paperRequest);

        StepVerifier.create(result)
                .verifyComplete();


        Assertions.assertTrue(captor.getValue().getApplyRasterization());
        verify(dao, times(1)).updateApplyRasterization(any(), any());
        verify(retryableErrorMessageHandler, times(1)).handleMessage(any(), any());
        verify(notRetryableErrorMessageHandler, never()).handleMessage(any(PnDeliveryRequest.class), any(PaperProgressStatusEventDto.class));
    }

    @Test
    void testHandleMessageWithApplyRasterizationNull() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();

        when(dao.updateApplyRasterization(entity.getRequestId(), true))
                .thenReturn(Mono.just(mock(UpdateItemResponse.class)));

        ArgumentCaptor<PnDeliveryRequest> captor = ArgumentCaptor.forClass(PnDeliveryRequest.class);
        when(retryableErrorMessageHandler.handleMessage(captor.capture(), any(PaperProgressStatusEventDto.class)))
                .thenReturn(Mono.empty());

        Mono<Void> result = proxyCON996MessageHandler.handleMessage(entity, paperRequest);

        StepVerifier.create(result)
                .verifyComplete();

        Assertions.assertTrue(captor.getValue().getApplyRasterization());
        verify(dao, times(1)).updateApplyRasterization(any(), any());
        verify(retryableErrorMessageHandler, times(1)).handleMessage(any(), any());
        verify(notRetryableErrorMessageHandler, never()).handleMessage(any(PnDeliveryRequest.class), any(PaperProgressStatusEventDto.class));
    }
}