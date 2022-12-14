package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendResponse;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getPrepareRequest())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testSendPaperSend(){
        SendResponse response = new SendResponse();
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-send/12345ABC";
        Mockito.when(paperMessagesService.executionPaper(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getSendRequest())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testRetrievePaperPrepareRequest(){
        PrepareEvent response = new PrepareEvent();
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/12345ABC";
        Mockito.when(paperMessagesService.retrievePaperPrepareRequest(Mockito.anyString()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    private SendRequest getSendRequest() {
        SendRequest sendRequest =new SendRequest();
        List<String> attachmentUrls = new ArrayList<String>();
        AnalogAddress analogAddress= new AnalogAddress();
        String s ="url12345";
        attachmentUrls.add(s);

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");

        sendRequest.setRequestPaId("12345abcd");
        sendRequest.setClientRequestTimeStamp(new Date());
        sendRequest.setSenderAddress(analogAddress);
        sendRequest.setArAddress(analogAddress);
        sendRequest.setProductType(ProductTypeEnum.RN_AR);
        sendRequest.setIun("iun");
        sendRequest.setRequestId("12345abcd");
        sendRequest.setReceiverFiscalCode("FDR3764GBC501A");
        sendRequest.setReceiverType("type");
        sendRequest.setReceiverAddress(analogAddress);
        sendRequest.setPrintType("pr");
        sendRequest.setAttachmentUrls(attachmentUrls);
        return sendRequest;
    }


    private PrepareRequest getPrepareRequest() {
        PrepareRequest prepareRequest = new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= new AnalogAddress();
        String s ="url12345";
        attachmentUrls.add(s);

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");

        prepareRequest.setRequestId("12345ABC");
        prepareRequest.setAttachmentUrls(attachmentUrls);
        prepareRequest.setDiscoveredAddress(analogAddress);
        prepareRequest.setIun("iun");
        prepareRequest.setReceiverAddress(analogAddress);
        prepareRequest.setPrintType("BN_FRONTE_RETRO");
        prepareRequest.setRelatedRequestId(null);
        prepareRequest.setProposalProductType(ProposalTypeEnum.AR);
        prepareRequest.setReceiverFiscalCode("FRMTTR76M06B715E");
        prepareRequest.setReceiverType("PF");
        return prepareRequest;
    }
}
