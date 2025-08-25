package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verifyNoInteractions;
import static reactor.core.publisher.Mono.when;

@ExtendWith(MockitoExtension.class)
public class PcRetryServiceImplTest {

    @InjectMocks
    private PcRetryServiceImpl pcRetryService;

    @Mock
    private PcRetryUtils pcRetryUtils;

    @Mock
    RequestDeliveryDAO requestDeliveryDAO;

    @Mock
    PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;

    @Test
    void getPcRetryFoundTest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        pnDeliveryRequest.setDriverCode("driver1");
        Mockito.when(requestDeliveryDAO.getByRequestId("requestId")).thenReturn(Mono.just(pnDeliveryRequest));
        PaperChannelDeliveryDriver paperChannelDeliveryDriver = new PaperChannelDeliveryDriver();
        paperChannelDeliveryDriver.setUnifiedDeliveryDriver("unifiedDriver1");
        Mockito.when(paperChannelDeliveryDriverDAO.getByDeliveryDriverId("driver1")).thenReturn(Mono.just(paperChannelDeliveryDriver));

        PcRetryResponse response = new PcRetryResponse();
        response.setParentRequestId("requestId");
        response.setDeliveryDriverId("unifiedDriver1");
        response.setRetryFound(true);
        response.setRequestId("requestId_.PCRETRY_2");
        response.setPcRetry("PCRETRY_2");
        Mockito.when(pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse("requestId", "unifiedDriver1", pnDeliveryRequest))
                .thenReturn(Mono.just(response));

        StepVerifier.create(pcRetryService.getPcRetry("requestId"))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void getPcRetryNotFoundTest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        pnDeliveryRequest.setDriverCode("driver1");
        Mockito.when(requestDeliveryDAO.getByRequestId("requestId")).thenReturn(Mono.just(pnDeliveryRequest));
        PaperChannelDeliveryDriver paperChannelDeliveryDriver = new PaperChannelDeliveryDriver();
        paperChannelDeliveryDriver.setUnifiedDeliveryDriver("unifiedDriver1");
        Mockito.when(paperChannelDeliveryDriverDAO.getByDeliveryDriverId("driver1")).thenReturn(Mono.just(paperChannelDeliveryDriver));

        PcRetryResponse response = new PcRetryResponse();
        response.setParentRequestId("requestId");
        response.setDeliveryDriverId("unifiedDriver1");
        response.setRetryFound(false);
        Mockito.when(pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse("requestId", "unifiedDriver1", pnDeliveryRequest))
                .thenReturn(Mono.just(response));

        StepVerifier.create(pcRetryService.getPcRetry("requestId"))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void getPcRetryRequestIdNotFoundTest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        pnDeliveryRequest.setDriverCode("driver1");
        Mockito.when(requestDeliveryDAO.getByRequestId("requestId")).thenReturn(Mono.empty());

        StepVerifier.create(pcRetryService.getPcRetry("requestId"))
                .expectError(PnGenericException.class)
                .verify();

        verifyNoInteractions(paperChannelDeliveryDriverDAO);
        verifyNoInteractions(pcRetryUtils);
    }

    @Test
    void getPcRetryDriverNotFoundTest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        pnDeliveryRequest.setDriverCode("driver1");
        Mockito.when(requestDeliveryDAO.getByRequestId("requestId")).thenReturn(Mono.just(pnDeliveryRequest));
        Mockito.when(paperChannelDeliveryDriverDAO.getByDeliveryDriverId("driver1")).thenReturn(Mono.empty());

        StepVerifier.create(pcRetryService.getPcRetry("requestId"))
                .expectError(PnGenericException.class)
                .verify();

        verifyNoInteractions(pcRetryUtils);
    }

}
