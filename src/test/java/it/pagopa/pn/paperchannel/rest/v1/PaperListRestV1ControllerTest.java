package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.rest.v1.dto.CapDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.service.PaperListService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@WebFluxTest(controllers = {PaperListRestV1Controller.class})
class PaperListRestV1ControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private PaperListService paperListService;

    @Test
    void testGetAllCap(){
        CapResponseDto response = new CapResponseDto();
        String path = "/paper-channel-bo/v1/cap";
        Mockito.when(paperListService.getAllCap(Mockito.anyString()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    /*private CapResponseDto getCapResponse(){
        CapResponseDto response = new CapResponseDto();
        List<CapDto> caps = new ArrayList<CapDto>();
        CapDto cap = new CapDto();
        cap.setCap("00166");
        caps.add(cap);
        return response;
    }*/
}