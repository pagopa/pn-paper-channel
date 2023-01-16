package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ExternalChannelClientTest  extends BaseTest.WithMockServer {

    @Autowired
    private ExternalChannelClient externalChannelClient;

    private SendRequest sendRequest;

    @BeforeEach
    public void setUp() {
        this.sendRequest = getRequest("abcd12345");
        sendRequest.setClientRequestTimeStamp(new Date());

    }

    @Test
    void testOK() {
        AnalogAddress arAddress = new AnalogAddress();
        arAddress.setFullname("abcdefg12345");
        arAddress.setAddress("via roma");
        arAddress.setCap("00065");
        arAddress.setCity("roma");

        this.sendRequest.getRequestId();
        this.sendRequest.setArAddress(arAddress);
        this.sendRequest.setProductType(ProductTypeEnum.RI_AR);
        externalChannelClient.sendEngageRequest(sendRequest).block();
    }

    @Test
    void testOKproductTypeRN_RS() {
        this.sendRequest.getRequestId();
        this.sendRequest.setProductType(ProductTypeEnum.RN_RS);
        externalChannelClient.sendEngageRequest(sendRequest).block();
    }

    @Test
    void testOKproductTypeRN_890() {
        this.sendRequest.getRequestId();
        this.sendRequest.setProductType(ProductTypeEnum.RN_890);
        externalChannelClient.sendEngageRequest(sendRequest).block();
    }


    @Test
    void testBadRequest() {
        this.sendRequest.getRequestId();
        externalChannelClient.sendEngageRequest(sendRequest)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.BAD_REQUEST);
                    return Mono.empty();
                }).block();
    }

    @Test
    void testConflict() {
        this.sendRequest.getRequestId();
        externalChannelClient.sendEngageRequest(sendRequest)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.CONFLICT);
                    return Mono.empty();
                }).block();
    }

    private SendRequest getRequest(String requestId) {
        SendRequest sendRequest = new SendRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress = new AnalogAddress();
        String s = "http://localhost:8080";
        attachmentUrls.add(s);

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");
        analogAddress.setNameRow2("Ettore");

        sendRequest.setRequestId(requestId);
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProductType(ProductTypeEnum.RN_AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        sendRequest.setSenderAddress(analogAddress);
        return sendRequest;
    }
}