package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.mapper.PrepareRequestMapper;
import it.pagopa.pn.paperchannel.model.PrepareRequestInt;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@WebFluxTest(controllers = InformalMessagesRestV1Controller.class)
class InformalMessagesRestV1ControllerTest {
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String BASE_PATH = "/paper-channel-private/v1/paper-deliveries-prepare/informal";
    private static final String URI_LOCATION_BASE = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/";

    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private PaperMessagesService paperMessagesService;
    @MockitoBean
    private PrepareRequestMapper prepareRequestMapper;

    @Test
    void sendInformalPrepareRequest_whenServiceReturnsValue_thenReturns200() {
        PaperChannelUpdate serviceResponse = new PaperChannelUpdate();

        Mockito.when(prepareRequestMapper.informalPrepareRequestToInternal(Mockito.any(), Mockito.anyString()))
                .thenReturn(new PrepareRequestInt());

        Mockito.when(paperMessagesService.preparePaperSync(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(serviceResponse));

        webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(getInformalPrepareRequest())
                .header("X-Client-Id", TEST_CLIENT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(InformalPrepareResponse.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals("12345ABC",response.getRequestId());
                });
    }

    @Test
    void sendInformalPrepareRequest_whenServiceReturnsEmpty_thenReturns201() {
        Mockito.when(prepareRequestMapper.informalPrepareRequestToInternal(Mockito.any(), Mockito.anyString()))
                .thenReturn(null);

        Mockito.when(paperMessagesService.preparePaperSync(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(getInformalPrepareRequest())
                .header("X-Client-Id", TEST_CLIENT_ID)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("Location", location -> {
                    Assertions.assertNotNull(location);
                    Assertions.assertTrue(location.contains(URI_LOCATION_BASE));
                })
                .expectBody(InformalPrepareResponse.class)
                .value(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertEquals("12345ABC",response.getRequestId());
                });
    }

    @Test
    void sendInformalPrepareRequest_whenMissingClientId_thenReturns400() {
        webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(getInformalPrepareRequest())
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verify(paperMessagesService, Mockito.never())
                .preparePaperSync(Mockito.anyString(), Mockito.any());
    }

    @Test
    void sendInformalPrepareRequest_whenEmptyClientId_thenReturns400() {
        webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(getInformalPrepareRequest())
                .header("X-Client-Id", "")
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verify(paperMessagesService, Mockito.never())
                .preparePaperSync(Mockito.anyString(), Mockito.any());
    }

    @Test
    void sendInformalPrepareRequest_whenEmptyRequestId_thenReturns400() {
        InformalPrepareRequest request = getInformalPrepareRequest();
        request.setRequestId("");

        webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(request)
                .header("X-Client-Id", TEST_CLIENT_ID)
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verify(paperMessagesService, Mockito.never())
                .preparePaperSync(Mockito.anyString(), Mockito.any());
    }

    private InformalPrepareRequest getInformalPrepareRequest() {
        InformalPrepareRequest request = new InformalPrepareRequest();

        List<String> attachmentUrls = new ArrayList<>();
        attachmentUrls.add("url12345");

        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");

        request.setRequestId("12345ABC");
        request.setAttachmentUrls(attachmentUrls);
        request.setReceiverAddress(analogAddress);
        request.setReceiverType("PF");
        request.setPrintType(InformalPrepareRequest.PrintTypeEnum.BN_FRONTE);
        request.setIun("IUN123");
        request.setProposalProductType(InformalProposalProductTypeEnum.RS);

        return request;
    }
}