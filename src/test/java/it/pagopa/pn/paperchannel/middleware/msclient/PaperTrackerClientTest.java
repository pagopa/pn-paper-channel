package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.NotificationReworkApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.PaperTrackerTrackingApi;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.impl.PaperTrackerClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaperTrackerClientTest  {


    private PaperTrackerClientImpl paperTrackerClient;
    private PaperTrackerTrackingApi paperTrackerTrackingApi;
    private NotificationReworkApi notificationReworkApi;

    @BeforeEach
    void setUp() {
        paperTrackerTrackingApi = mock(PaperTrackerTrackingApi.class);
        notificationReworkApi = mock(NotificationReworkApi.class);
        paperTrackerClient = new PaperTrackerClientImpl(paperTrackerTrackingApi, notificationReworkApi);
    }

    @Test
    void testInitTrackingOk(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        when(paperTrackerTrackingApi.initTracking(any())).thenReturn(Mono.empty());
        StepVerifier.create(paperTrackerClient.initPaperTracking("requestId", "PCRETRY_0", "AR","driver"))
                .verifyComplete();
    }

    @Test
    void testInitTrackingConflict(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        WebClientResponseException exception = WebClientResponseException.create(
                409, "Conflict", null, null, null);
        when(paperTrackerTrackingApi.initTracking(any())).thenReturn(Mono.error(exception));
        StepVerifier.create(paperTrackerClient.initPaperTracking("requestId", "PCRETRY_0","AR","driver"))
                .verifyError(PnIdConflictException.class);
    }

    @Test
    void testInitTrackingError(){
        WebClientResponseException exception = WebClientResponseException.create(
                500, "Conflict", null, null, null);
        when(paperTrackerTrackingApi.initTracking(any())).thenReturn(Mono.error(exception));
        StepVerifier.create(paperTrackerClient.initPaperTracking("requestId", "PCRETRY_0","AR","driver"))
                .verifyError(WebClientResponseException.class);
    }

    @Test
    void testInitNotificationReworkOk(){
        when(notificationReworkApi.initNotificationRework("reworkId","requestId")).thenReturn(Mono.empty());
        StepVerifier.create(paperTrackerClient.initNotificationRework("reworkId","requestId"))
                .verifyComplete();
    }

    @Test
    void testInitNotificationReworkNotFound(){
        WebClientResponseException exception = WebClientResponseException.create(
                404, "Not Found", null, null, null);
        when(notificationReworkApi.initNotificationRework("reworkId","requestId")).thenReturn(Mono.error(exception));
        StepVerifier.create(paperTrackerClient.initNotificationRework("reworkId","requestId"))
                .verifyError(WebClientResponseException.class);
    }


}
