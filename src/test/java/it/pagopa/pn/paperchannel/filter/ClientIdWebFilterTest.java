package it.pagopa.pn.paperchannel.filter;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnClientID;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.config.InstanceCreator.getPrepareRequest;

@SpringBootTest
@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ClientIdWebFilterTest extends BaseTest.WithOutLocalStackTest {
    private static final String DEFAULT_URL_TEST = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/12345ABC";
    private static final String PN_CLIENT_ID = "SERVICE-001";
    private static final String PN_CLIENT_ID_NOT_IN_DB = "PAPER-001";
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private PaperMessagesService paperMessagesService;
    @MockitoBean
    private PnClientDAO pnClientDAO;

    @BeforeEach
    void setUp(){
        Mockito.when(paperMessagesService.preparePaperSync(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(new PaperChannelUpdate()));

        PnClientID entity = new PnClientID();
        entity.setClientId(PN_CLIENT_ID);
        entity.setPrefix("001");
        Mockito.when(pnClientDAO.getByClientId(PN_CLIENT_ID))
                .thenReturn(Mono.just(entity));

        Mockito.when(pnClientDAO.getByClientId(PN_CLIENT_ID_NOT_IN_DB))
                .thenReturn(Mono.empty());
    }


    @Test
    void when_ClientIdHeaderNotPresent_Then_GoToService(){
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(DEFAULT_URL_TEST).build())
                .bodyValue(getPrepareRequest())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void when_ClientIdHeaderPresentWithEmptyValue_Throw_UnauthorizedException(){
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(DEFAULT_URL_TEST).build())
                .header(Const.HEADER_CLIENT_ID, "")
                .bodyValue(getPrepareRequest())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void when_ClientIdHeaderPresentWithValueButItsNotInDB_Throw_UnauthorizedException(){
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(DEFAULT_URL_TEST).build())
                .header(Const.HEADER_CLIENT_ID, PN_CLIENT_ID_NOT_IN_DB)
                .bodyValue(getPrepareRequest())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void when_ClientIdHeaderPresent_Then_GoTOService(){
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(DEFAULT_URL_TEST).build())
                .header(Const.HEADER_CLIENT_ID, PN_CLIENT_ID)
                .bodyValue(getPrepareRequest())
                .exchange()
                .expectStatus().isOk();
    }


}
