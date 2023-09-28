package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class F24ClientTest extends BaseTest.WithMockServer {


    @Autowired
    private F24Client f24Client;


    @Test
    void testOK(){
        RequestAcceptedDto responseDto = f24Client.preparePDF("REQUESTID", "SETID", 0, 100).block();
        Assertions.assertNotNull(responseDto);
        Assertions.assertNotNull(responseDto.getStatus());
    }

    @Test
    void testKO(){
        Mono<RequestAcceptedDto> mono = f24Client.preparePDF("REQUESTIDCONFLICT", "SETID", 0, 100);
        Assertions.assertThrows(PnAddressFlowException.class, () -> mono.block());
    }
}
