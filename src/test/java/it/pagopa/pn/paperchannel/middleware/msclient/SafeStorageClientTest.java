package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SafeStorageClientTest extends BaseTest.WithMockServer {


    @Autowired
    private SafeStorageClient safeStorageClient;


    @Test
    void test(){
        assertTrue(true);
    }

}
