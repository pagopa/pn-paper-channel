package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressOKDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NationalRegistryClientTest extends BaseTest.WithMockServer {


    @Autowired
    private NationalRegistryClient nationalRegistryClient;


    @Test
    void testOK(){
        AddressOKDto addressOKDtoMono = nationalRegistryClient.finderAddress("AAAZZZ00H00T000Z","PF").block();
        Assertions.assertNotNull(addressOKDtoMono);
        Assertions.assertNotNull(addressOKDtoMono.getCorrelationId());
    }

}
