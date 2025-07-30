package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.PaperTrackerEventApi;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.impl.PaperTrackerClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaperTrackerClientTest  {


    private PaperTrackerClientImpl paperTrackerClient;
    private PaperTrackerEventApi paperTrackerEventApi;

    @BeforeEach
    void setUp() {
        paperTrackerEventApi = mock(PaperTrackerEventApi.class);
        paperTrackerClient = new PaperTrackerClientImpl(paperTrackerEventApi);
    }

    @Test
    void testOk(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        when(paperTrackerEventApi.initTracking(any())).thenReturn(Mono.empty());
        StepVerifier.create(paperTrackerClient.initPaperTracking(pnDeliveryRequest,"driver"))
                .expectNext(pnDeliveryRequest)
                .verifyComplete();
    }

    @Test
    void testConflict(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        WebClientResponseException exception = WebClientResponseException.create(
                409, "Conflict", null, null, null);
        when(paperTrackerEventApi.initTracking(any())).thenReturn(Mono.error(exception));
        StepVerifier.create(paperTrackerClient.initPaperTracking(pnDeliveryRequest,"driver"))
                .verifyError(PnIdConflictException.class);
    }

    @Test
    void testError(){
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        WebClientResponseException exception = WebClientResponseException.create(
                500, "Conflict", null, null, null);
        when(paperTrackerEventApi.initTracking(any())).thenReturn(Mono.error(exception));
        StepVerifier.create(paperTrackerClient.initPaperTracking(pnDeliveryRequest,"driver"))
                .verifyError(WebClientResponseException.class);
    }
}
