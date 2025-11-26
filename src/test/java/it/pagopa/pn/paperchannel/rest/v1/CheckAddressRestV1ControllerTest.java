package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CheckAddressResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.service.CheckAddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {CheckAddressRestV1Controller.class})
class CheckAddressRestV1ControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private CheckAddressService checkAddressService;
    @MockBean
    private PnClientDAO pnClientDAO;

    @Test
    void testCheckAddressRequestOk() {
        String path = "/paper-channel-private/v1/{requestId}/check-address";
        Instant now = Instant.now();

        CheckAddressResponse response = new CheckAddressResponse();
        response.setRequestId("requestId2");
        response.setEndValidity(now);

        when(checkAddressService.checkAddressRequest("requestId2"))
                .thenReturn(Mono.just(response));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("requestId", "requestId2").build("requestId2"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody().json("{\"requestId\":\"requestId2\",\"endValidity\":\"" + now.toString() + "\"}");
    }

    @Test
    void testCheckAddressRequestKo() {
        String path = "/paper-channel-private/v1/{requestId}/check-address";

        when(checkAddressService.checkAddressRequest("requestId2"))
                .thenReturn(Mono.empty());
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("requestId", "requestId2").build("requestId2"))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

}