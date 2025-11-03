package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.PaperTrackerTrackingApi;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.PaperTrackerClient;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class NotificationReworkServiceImplTest {

    @InjectMocks
    private NotificationReworkServiceImpl service;
    @Mock
    private PcRetryServiceImpl pcRetryService;
    @Mock
    private MetaDematCleaner metaDematCleaner;
    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    private PaperTrackerClient paperTrackerClient;


    @Test
    void testInitNotificationReworkSuccess() {
        String requestId = "REQ123";
        String reworkId = "REW456";
        String requestIdWithoutPcRetry = "REQ123";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);

        Mockito.when(pcRetryService.getPrefixRequestId(requestId)).thenReturn(requestIdWithoutPcRetry);
        Mockito.when(metaDematCleaner.clean(requestIdWithoutPcRetry)).thenReturn(Mono.empty());
        Mockito.when(requestDeliveryDAO.getByRequestId(requestIdWithoutPcRetry)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.cleanDataForNotificationRework(
                Mockito.any(PnDeliveryRequest.class),
                Mockito.eq(reworkId)
        )).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(paperTrackerClient.initNotificationRework(reworkId, requestId)).thenReturn(Mono.empty());

        StepVerifier.create(service.initNotificationRework(requestId, reworkId))
                .verifyComplete();

        Mockito.verify(pcRetryService, Mockito.times(1)).getPrefixRequestId(requestId);
        Mockito.verify(metaDematCleaner, Mockito.times(1)).clean(requestIdWithoutPcRetry);
        Mockito.verify(requestDeliveryDAO, Mockito.times(1)).getByRequestId(requestIdWithoutPcRetry);
        Mockito.verify(requestDeliveryDAO, Mockito.times(1)).cleanDataForNotificationRework(
                Mockito.any(PnDeliveryRequest.class), Mockito.eq(reworkId));
        Mockito.verify(paperTrackerClient, Mockito.times(1)).initNotificationRework(reworkId, requestId);
    }

    @Test
    void testInitNotificationReworkRequestNotFound() {
        String requestId = "REQ123";
        String reworkId = "REW456";
        String requestIdWithoutPcRetry = "REQ123";

        Mockito.when(pcRetryService.getPrefixRequestId(requestId)).thenReturn(requestIdWithoutPcRetry);
        Mockito.when(metaDematCleaner.clean(requestIdWithoutPcRetry)).thenReturn(Mono.empty());
        Mockito.when(requestDeliveryDAO.getByRequestId(requestIdWithoutPcRetry)).thenReturn(Mono.empty());

        StepVerifier.create(service.initNotificationRework(requestId, reworkId))
                .expectErrorSatisfies(error -> {
                    assert error instanceof PnGenericException;
                    PnGenericException ex = (PnGenericException) error;
                    assert ex.getExceptionType() == ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
                    assert ex.getHttpStatus() == HttpStatus.NOT_FOUND;
                })
                .verify();

        Mockito.verify(paperTrackerClient, Mockito.never()).initNotificationRework(Mockito.any(), Mockito.any());
    }

    @Test
    void testInitNotificationReworkNotificationNotFoundOnPaperTracker() {
        String requestId = "REQ123";
        String reworkId = "REW456";
        String requestIdWithoutPcRetry = "REQ123";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);

        Mockito.when(pcRetryService.getPrefixRequestId(requestId)).thenReturn(requestIdWithoutPcRetry);
        Mockito.when(metaDematCleaner.clean(requestIdWithoutPcRetry)).thenReturn(Mono.empty());
        Mockito.when(requestDeliveryDAO.getByRequestId(requestIdWithoutPcRetry)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.cleanDataForNotificationRework(
                Mockito.any(PnDeliveryRequest.class),
                Mockito.any()
        )).thenReturn(Mono.just(deliveryRequest));

        WebClientResponseException notFoundException = new WebClientResponseException(
                404, "Not Found", null, null, null, null);

        Mockito.when(paperTrackerClient.initNotificationRework(reworkId, requestId))
                .thenReturn(Mono.error(notFoundException));

        StepVerifier.create(service.initNotificationRework(requestId, reworkId))
                .expectErrorSatisfies(error -> {
                    assert error instanceof WebClientResponseException;
                    WebClientResponseException ex = (WebClientResponseException) error;
                    assert ex.getStatusCode() == HttpStatus.NOT_FOUND;
                })
                .verify();
    }

    @Test
    void testInitNotificationReworkCleanDataError() {
        String requestId = "REQ123";
        String reworkId = "REW456";
        String requestIdWithoutPcRetry = "REQ123";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);

        Mockito.when(pcRetryService.getPrefixRequestId(requestId)).thenReturn(requestIdWithoutPcRetry);
        Mockito.when(metaDematCleaner.clean(requestIdWithoutPcRetry)).thenReturn(Mono.empty());
        Mockito.when(requestDeliveryDAO.getByRequestId(requestIdWithoutPcRetry)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.cleanDataForNotificationRework(
                Mockito.any(PnDeliveryRequest.class),
                Mockito.eq(reworkId)
        )).thenReturn(Mono.error(new RuntimeException("Clean data error")));

        StepVerifier.create(service.initNotificationRework(requestId, reworkId))
                .expectError(RuntimeException.class)
                .verify();

        Mockito.verify(paperTrackerClient, Mockito.never()).initNotificationRework(Mockito.any(), Mockito.any());
    }

    @Test
    void testInitNotificationReworkKeepsDeliveryRequestNotExist() {
        String requestId = "REQ123";
        String reworkId = "REW456";
        String requestIdWithoutPcRetry = "REQ123";

        Mockito.when(pcRetryService.getPrefixRequestId(requestId)).thenReturn(requestIdWithoutPcRetry);
        Mockito.when(metaDematCleaner.clean(requestIdWithoutPcRetry)).thenReturn(Mono.empty());

        // Simula che la richiesta non esista nel database
        Mockito.when(requestDeliveryDAO.getByRequestId(requestIdWithoutPcRetry)).thenReturn(Mono.empty());

        StepVerifier.create(service.initNotificationRework(requestId, reworkId))
                .expectErrorSatisfies(error -> {
                    assert error instanceof PnGenericException;
                    PnGenericException ex = (PnGenericException) error;
                    assert ex.getHttpStatus() == HttpStatus.NOT_FOUND;
                })
                .verify();
    }
}