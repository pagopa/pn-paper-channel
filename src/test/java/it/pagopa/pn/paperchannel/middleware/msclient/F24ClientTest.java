package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.NumberOfPagesResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class F24ClientTest extends BaseTest.WithMockServer {

    private static final String REQUEST_ID = "REQUESTID";
    private static final String SET_ID = "SETID";
    private static final String RECIPIENT_INDEX = "0";

    @Autowired
    private F24Client f24Client;


    @Test
    void testOK(){
        RequestAcceptedDto responseDto = f24Client.preparePDF(REQUEST_ID, SET_ID, RECIPIENT_INDEX, 100).block();
        Assertions.assertNotNull(responseDto);
        Assertions.assertNotNull(responseDto.getStatus());
    }

    @Test
    void testKO(){
        Mono<RequestAcceptedDto> mono = f24Client.preparePDF("REQUESTIDCONFLICT", SET_ID, RECIPIENT_INDEX, 100);
        Assertions.assertThrows(WebClientResponseException.class, mono::block);
    }

    @Test
    void testGetNumberOfPagesOK() {
        NumberOfPagesResponseDto numberOfPagesResponseDtoMono = f24Client.getNumberOfPages(SET_ID, RECIPIENT_INDEX).block();
        Assertions.assertNotNull(numberOfPagesResponseDtoMono);
        Assertions.assertNotNull(numberOfPagesResponseDtoMono.getNumberOfPages());
    }

    @Test
    void testGetNumberOfPagesKO() {
        Mono<NumberOfPagesResponseDto> numberOfPagesResponseDtoMono = f24Client.getNumberOfPages("BADSETID", RECIPIENT_INDEX);
        Assertions.assertThrows(WebClientResponseException.class, numberOfPagesResponseDtoMono::block);
    }
}
