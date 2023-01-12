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
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.service.impl.PaperMessagesServiceImpl;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class PaperMessagesServiceTest extends BaseTest {


    @InjectMocks
    private PaperMessagesServiceImpl paperMessagesService;

    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;

    @Mock
    private AddressDAO addressDAO;

    @Mock
    private NationalRegistryClient nationalRegistryClient;

    @Mock
    private ExternalChannelClient externalChannelClient;

    @Mock
    private PrepareAsyncServiceImpl prepareAsyncService;

    @Mock
    private SqsSender sqsSender;

    private PnDeliveryRequest deliveryRequestInProcessing;
    private PnDeliveryRequest deliveryRequestTakingCharge;

    @BeforeEach
    void setUp(){
        this.deliveryRequestInProcessing = getDeliveryRequest("123-adb-567", StatusDeliveryEnum.IN_PROCESSING);
        this.deliveryRequestTakingCharge = getDeliveryRequest("123-adb-KIJ", StatusDeliveryEnum.TAKING_CHARGE);
    }

    @Test
    void retrievePrepareEntityOkAddressNullTest(){
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(deliveryRequestInProcessing));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.empty());
        PrepareEvent prepareEvent = paperMessagesService.retrievePaperPrepareRequest("abcde12345").block();
        assertNotNull(prepareEvent);
        assertEquals(StatusCodeEnum.PROGRESS, prepareEvent.getStatusCode());
        assertEquals(prepareEvent.getProductType(), deliveryRequestInProcessing.getProposalProductType());
        assertNull(prepareEvent.getReceiverAddress());
    }


    @Test
    void paperMessagesServiceTest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequest()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress("abcde12345")));
        PrepareEvent prepareEvent = paperMessagesService.retrievePaperPrepareRequest("abcde12345").block();
        assertNotNull(prepareEvent);
        Assertions.assertEquals(prepareEvent.getStatusCode(), StatusCodeEnum.PROGRESS);
    }

    @Test
    void paperMessagesServiceTest2() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequestUntraceable()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress("abcde12345")));
        PrepareEvent prepareEvent = paperMessagesService.retrievePaperPrepareRequest("abcde12345").block();
        assertNotNull(prepareEvent);
        Assertions.assertEquals(prepareEvent.getStatusCode(), StatusCodeEnum.KOUNREACHABLE);
    }

    @Test
    void paperMessagesServiceTestErrorRequest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.empty());
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress("abcde12345")));
        StepVerifier.create(paperMessagesService.retrievePaperPrepareRequest("abcde12345")).expectError(PnGenericException.class).verify();
    }

    //-------------TEST DA SISTEMARE----------
   // @Test
    void executionPaperTest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequest()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress("abcde12345")));
        Mockito.doNothing().when(externalChannelClient).sendEngageRequest(Mockito.any()).block();
        paperMessagesService.executionPaper("abcde12345",getRequest()).block();

    }

    @Test
    void paperAsyncEntityAndAddressOKTest() {
        PnAddress address = getPnAddress(deliveryRequestTakingCharge.getRequestId());
        Mockito.when(requestDeliveryDAO.getByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(deliveryRequestTakingCharge));
        Mockito.when(addressDAO.findByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(address));
        PaperChannelUpdate response = paperMessagesService.preparePaperSync(deliveryRequestTakingCharge.getRequestId(), getRequestOK()).block();
        log.info(response.toString());
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
        deliveryRequest.setProposalProductType("AR");
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


    private PnAddress getPnAddress(String requestId){
        PnAddress pnAddress = new PnAddress();
        pnAddress.setRequestId(requestId);
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

    private PnDeliveryRequest getDeliveryRequest(String requestId, StatusDeliveryEnum status){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("PDFURL");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("http://localhost:8080");
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
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("iun");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode(status.getCode());
        deliveryRequest.setStatusDetail(status.getDescription());
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }
}
