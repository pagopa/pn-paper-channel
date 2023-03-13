package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;

import it.pagopa.pn.paperchannel.model.AttachmentInfo;
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
import java.util.Map;

class ExternalChannelClientTest  extends BaseTest.WithMockServer {

    @Autowired
    private ExternalChannelClient externalChannelClient;

    private final SendRequest sendRequest = new SendRequest();
    private final List<String> attachmentUrls = new ArrayList<>();
    private final AnalogAddress analogAddress = new AnalogAddress();

    @BeforeEach
    public void setUp() { inizialize();}

    @Test
    void testOK() {
        externalChannelClient.sendEngageRequest(sendRequest, new ArrayList<>()).block();
        Assertions.assertTrue(true);
    }

    @Test
    void testBadRequest() {
        sendRequest.setRequestId(null);
        WebClientResponseException exception = Assertions.assertThrows(WebClientResponseException.class, ()-> {
            externalChannelClient.sendEngageRequest(sendRequest, new ArrayList<>()).block();
        });
        Assertions.assertEquals(exception.getStatusCode().value(), HttpStatus.BAD_REQUEST.value());
    }

    private void inizialize(){
        attachmentUrls.add("safestorage://PN_AAR-0002-GR7Z-3UBM-81QT-1QWV");

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");
        analogAddress.setNameRow2("Ettore");

        sendRequest.setRequestId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        sendRequest.setReceiverFiscalCode("PLOMRC01P30L736Y5");
        sendRequest.setProductType(ProductTypeEnum.RIR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setRequestPaId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        sendRequest.setSenderAddress(analogAddress);
        sendRequest.setArAddress(analogAddress);
        sendRequest.setClientRequestTimeStamp(new Date());
        sendRequest.setProductType(ProductTypeEnum.RIR);

    }
}