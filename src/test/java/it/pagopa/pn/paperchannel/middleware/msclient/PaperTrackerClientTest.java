package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
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
    private PaperTrackerTrackingApi PaperTrackerTrackingApi;

    @BeforeEach
    void setUp() {
        PaperTrackerTrackingApi = mock(PaperTrackerTrackingApi.class);
        paperTrackerClient = new PaperTrackerClientImpl(PaperTrackerTrackingApi);
    }

    @Test
    void testOk(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        when(PaperTrackerTrackingApi.initTracking(any())).thenReturn(Mono.empty());
        StepVerifier.create(paperTrackerClient.initPaperTracking("requestId.PCRETRY_0","AR","driver"))
                .verifyComplete();
    }

    @Test
    void testConflict(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        WebClientResponseException exception = WebClientResponseException.create(
                409, "Conflict", null, null, null);
        when(PaperTrackerTrackingApi.initTracking(any())).thenReturn(Mono.error(exception));
        StepVerifier.create(paperTrackerClient.initPaperTracking("requestId.PCRETRY_0","AR","driver"))
                .verifyError(PnIdConflictException.class);
    }

    @Test
    void testError(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        WebClientResponseException exception = WebClientResponseException.create(
                500, "Conflict", null, null, null);
        when(PaperTrackerTrackingApi.initTracking(any())).thenReturn(Mono.error(exception));
        StepVerifier.create(paperTrackerClient.initPaperTracking("requestId.PCRETRY_0","AR","driver"))
                .verifyError(WebClientResponseException.class);
    }
}
