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

import java.util.Date;

class ExternalChannelClientTest  extends BaseTest.WithMockServer {

    @Autowired
    private ExternalChannelClient externalChannelClient;

    private SendRequest sendRequest;

    @BeforeEach
    public void setUp(){
        this.sendRequest=new SendRequest();
        sendRequest.setClientRequestTimeStamp(new Date());
        sendRequest.setProductType(ProductTypeEnum.RI_AR);
        sendRequest.setReceiverAddress(new AnalogAddress());
        sendRequest.setSenderAddress(new AnalogAddress());
    }
    @Test
    void testOK(){
        this.sendRequest.setRequestId("abcd12345");
        externalChannelClient.sendEngageRequest(sendRequest).block();
    }
    @Test
    void testBadRequest(){
        this.sendRequest.setRequestId("abcd12345");
        externalChannelClient.sendEngageRequest(sendRequest)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.BAD_REQUEST);
                    return Mono.empty();
                }).block();
    }
    @Test
    void testConflict(){
        this.sendRequest.setRequestId("abcd12345");
        externalChannelClient.sendEngageRequest(sendRequest)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.CONFLICT);
                    return Mono.empty();
                }).block();
    }
}
