package it.pagopa.pn.paperchannel.middleware.msclient;

import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressOKDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class NationalRegistryClientTest extends BaseTest.WithMockServer {


    @Autowired
    private NationalRegistryClient nationalRegistryClient;


    @Test
    void testOK(){
        AddressOKDto addressOKDtoMono = nationalRegistryClient.finderAddress("CORR1", "CODICEFISCALE200","PF").block();
        Assertions.assertNotNull(addressOKDtoMono);
        Assertions.assertNotNull(addressOKDtoMono.getCorrelationId());
    }

    @Test
    void testWithRetryForConnectionTimeout(){
        AddressOKDto addressOKDtoMono = nationalRegistryClient.finderAddress("CORR1", "CODICEFISCALE_200_WITH_DELAY","PF").block();
        Assertions.assertNotNull(addressOKDtoMono);
        Assertions.assertNotNull(addressOKDtoMono.getCorrelationId());
    }

    @Test
    void testWithRetryForSocketClose(){
        AddressOKDto addressOKDtoMono = nationalRegistryClient.finderAddress("CORR1", "CODICEFISCALE_SOCKET_CLOSE","PF").block();
        Assertions.assertNotNull(addressOKDtoMono);
        Assertions.assertNotNull(addressOKDtoMono.getCorrelationId());
    }

    @Test
    void testWithExceededRetryForConnectionTimeout(){
        nationalRegistryClient.finderAddress("CORR1", "CODICEFISCALE_EXCEEDED_RETRY","PF")
                .onErrorResume(Exception.class, ex -> {
                    Assertions.assertInstanceOf(ReadTimeoutException.class, ex.getCause());
                    return Mono.empty();
                }).block();
    }

    @Test
    void testErrorBadRequest(){
        nationalRegistryClient.finderAddress("CORR1", "CODICEFISCALE400","PF")
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.BAD_REQUEST);
                    return Mono.empty();
                }).block();
    }

    @Test
    void testErrorNotFound(){
        nationalRegistryClient.finderAddress("CORR1", "CODICEFISCALE404","PF")
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.NOT_FOUND);
                    return Mono.empty();
                }).block();
    }

    @Test
    void testInternalServerError(){
        nationalRegistryClient.finderAddress("CORR1", "CODICEFISCALE500","PF")
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
                    return Mono.empty();
                }).block();
    }

}
