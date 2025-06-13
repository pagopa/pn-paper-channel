package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.service.PaperListService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {PaperListRestV1Controller.class})
class PaperListRestV1ControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private PaperListService paperListService;
    @MockitoBean
    private PnClientDAO pnClientDAO;

    @Test
    void testGetAllCap() {
        CapResponseDto response = new CapResponseDto();
        String path = "/paper-channel-bo/v1/cap";
        Mockito.when(paperListService.getAllCap(Mockito.anyString()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("value", "00100")
                        .build())
                .exchange()
                .expectStatus().isOk();
    }

}