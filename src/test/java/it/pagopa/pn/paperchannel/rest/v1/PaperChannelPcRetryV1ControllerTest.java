package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.service.PcRetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {PaperChannelPcRetryV1Controller.class})
public class PaperChannelPcRetryV1ControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private PcRetryService pcRetryService;
    @MockBean
    private PnClientDAO pnClientDAO;


    @Test
    void testPcRetryFound(){
        String path = "/paper-channel-private/v1/b2b/pc-retry/{requestId}";

        when(pcRetryService.getPcRetry("requestId1", false))
                .thenReturn(Mono.just(getPcRetryResponse(true)));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("checkApplyRasterization", Boolean.FALSE).build("requestId1"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody().json("{\"requestId\":\"requestId1.PCRETRY_2\",\"parentRequestId\":\"requestId1\",\"deliveryDriverId\":\"driver1\",\"pcRetry\":\"PCRETRY_\",\"retryFound\":true}");
    }

    @Test
    void testPcRetryNotFound(){
        String path = "/paper-channel-private/v1/b2b/pc-retry/{requestId}";

        when(pcRetryService.getPcRetry("requestId1", false))
                .thenReturn(Mono.just(getPcRetryResponse(false)));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("checkApplyRasterization", Boolean.FALSE).build("requestId1"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody().json("{\"requestId\":null,\"parentRequestId\":\"requestId1\",\"deliveryDriverId\":\"driver1\",\"pcRetry\":null,\"retryFound\":false}");
    }

    @Test
    void testPcRetryCON996Found(){
        String path = "/paper-channel-private/v1/b2b/pc-retry/{requestId}";

        when(pcRetryService.getPcRetry("requestId1", true))
                .thenReturn(Mono.just(getPcRetryResponse(true)));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("checkApplyRasterization", Boolean.TRUE).build("requestId1"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody().json("{\"requestId\":\"requestId1.PCRETRY_2\",\"parentRequestId\":\"requestId1\",\"deliveryDriverId\":\"driver1\",\"pcRetry\":\"PCRETRY_\",\"retryFound\":true}");
    }

    @Test
    void testPcRetryCON996NotFound(){
        String path = "/paper-channel-private/v1/b2b/pc-retry/{requestId}";

        when(pcRetryService.getPcRetry("requestId1", true))
                .thenReturn(Mono.just(getPcRetryResponse(false)));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("checkApplyRasterization", Boolean.TRUE).build("requestId1"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody().json("{\"requestId\":null,\"parentRequestId\":\"requestId1\",\"deliveryDriverId\":\"driver1\",\"pcRetry\":null,\"retryFound\":false}");
    }


    private static PcRetryResponse getPcRetryResponse(boolean found) {
        PcRetryResponse pcRetryResponse = new PcRetryResponse();
        pcRetryResponse.setParentRequestId("requestId1");
        pcRetryResponse.setDeliveryDriverId("driver1");
        if(found){
            pcRetryResponse.setPcRetry("PCRETRY_");
            pcRetryResponse.setRetryFound(true);
            pcRetryResponse.setRequestId("requestId1.PCRETRY_2");
        }else{
            pcRetryResponse.setRetryFound(false);
        }
        return pcRetryResponse;
    }
}
