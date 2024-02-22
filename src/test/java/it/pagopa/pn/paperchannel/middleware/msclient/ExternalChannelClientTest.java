package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.Const.PN_AAR;

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
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.setDocumentType(PN_AAR);
        externalChannelClient.sendEngageRequest(sendRequest, List.of(attachmentInfo)).block();
        StepVerifier.create(externalChannelClient.sendEngageRequest(sendRequest, List.of(attachmentInfo)))
                        .verifyComplete();
    }

    @Test
    void testBadRequest() {
        sendRequest.setRequestId(null);
        StepVerifier.create(externalChannelClient.sendEngageRequest(sendRequest, new ArrayList<>()))
                .expectErrorMatches(ex -> ex instanceof WebClientResponseException e && e.getStatusCode().value() == 400)
                .verify();
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
        sendRequest.setClientRequestTimeStamp(Instant.now());
        sendRequest.setProductType(ProductTypeEnum.RIR);

    }
}