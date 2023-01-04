package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {PaperMessagesRestV1Controller.class})
class PaperMessagesRestV1ControllerTest {


    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private PaperMessagesService paperMessagesService;

    @Test
    void testSendPaperPrepare(){
        PaperChannelUpdate response = new PaperChannelUpdate();
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/12345ABC";
        Mockito.when(paperMessagesService.preparePaperSync(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(response));

        PrepareRequest prepareRequest = new PrepareRequest();

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(prepareRequest)
                .exchange()
                .expectStatus().isOk();
    }


}
