package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.impl.PaperMessagesServiceImpl;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

class PaperMessagesServiceTest extends BaseTest {


    @InjectMocks
    PaperMessagesServiceImpl paperMessagesService;

    @Mock
    RequestDeliveryDAO requestDeliveryDAO;

    @Mock
    PnDeliveryRequest pnDeliveryRequestMono;

    @Mock
    private AddressDAO addressDAO;



    @Mock
    private NationalRegistryClient nationalRegistryClient;

    @Spy
    private ExternalChannelClient externalChannelClient;

    @Mock
    private PrepareAsyncServiceImpl prepareAsyncService;

    @Mock
    private SqsSender sqsSender;

    @Test
    void paperMessagesServiceTest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequest()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress()));
        PrepareEvent prepareEvent = paperMessagesService.retrievePaperPrepareRequest("abcde12345").block();
        Assertions.assertNotNull(prepareEvent);
        Assertions.assertEquals(prepareEvent.getStatusCode(), StatusCodeEnum.PROGRESS);
    }

    @Test
    void paperMessagesServiceTest2() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequestUntraceable()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress()));
        PrepareEvent prepareEvent = paperMessagesService.retrievePaperPrepareRequest("abcde12345").block();
        Assertions.assertNotNull(prepareEvent);
        Assertions.assertEquals(prepareEvent.getStatusCode(), StatusCodeEnum.KOUNREACHABLE);
    }

    @Test
    void paperMessagesServiceTestErrorRequest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.empty());
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress()));
        StepVerifier.create(paperMessagesService.retrievePaperPrepareRequest("abcde12345")).expectError(PnGenericException.class).verify();
    }

    @Test
    void paperMessagesServiceTestErrorAddress() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequest()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.empty());
        StepVerifier.create(paperMessagesService.retrievePaperPrepareRequest("abcde12345")).expectError(PnGenericException.class).verify();
    }

    //-------------TEST DA SISTEMARE----------
   // @Test
    void executionPaperTest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequest()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress()));
        Mockito.doNothing().when(externalChannelClient).sendEngageRequest(Mockito.any()).block();
        paperMessagesService.executionPaper("abcde12345",getRequest()).block();

    }

    @Test
    void finderAddressAndSaveTest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("12345abcde")).thenReturn(Mono.just(getEntityOK()));
        Mockito.when(addressDAO.findByRequestId("12345abcde")).thenReturn(Mono.just(getPnAddress()));
        paperMessagesService.preparePaperSync("12345abcde", getRequestOK()).block();

    }

    private SendRequest getRequest(){
        SendRequest sendRequest= new SendRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= new AnalogAddress();
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
        sendRequest.setRequestId("12345abcde");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProductType(ProductTypeEnum.RN_AR);
        sendRequest.setReceiverType("RT");
        sendRequest.setPrintType("PT");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        return sendRequest;
    }
    private PnDeliveryRequest gePnDeliveryRequest(){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");

        deliveryRequest.setAddressHash(address.convertToHash());
        deliveryRequest.setRequestId("12345abcde");
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("RT");
        deliveryRequest.setIun("");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode("PC000");
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }

    private PnDeliveryRequest gePnDeliveryRequestUntraceable(){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");

        deliveryRequest.setAddressHash(address.convertToHash());
        deliveryRequest.setRequestId("12345abcde");
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("RT");
        deliveryRequest.setIun("");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode("PC010");
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        List<PnAttachmentInfo> attachments;
        return deliveryRequest;
    }


    private PnAddress getPnAddress(){
        PnAddress pnAddress = new PnAddress();
        pnAddress.setRequestId("12345abcde");
        pnAddress.setAddress("via roma");
        pnAddress.setAddressRow2("via lazio");
        pnAddress.setCap("00061");
        pnAddress.setCity("roma");
        pnAddress.setCity2("viterbo");
        pnAddress.setCountry("italia");
        pnAddress.setPr("PR");
        pnAddress.setFullName("Ettore Fieramosca");
        pnAddress.setNameRow2("Ettore");
        return pnAddress;
    }

    private PrepareRequest getRequestOK(){
        PrepareRequest sendRequest= new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= new AnalogAddress();
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

        sendRequest.setRequestId("12345abcde");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProposalProductType(ProposalTypeEnum.AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        return sendRequest;
    }

    private PnDeliveryRequest getEntityOK(){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");

        deliveryRequest.setAddressHash(address.convertToHash());
        deliveryRequest.setRequestId("12345abcde");
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("iun");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode("");
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }
}
