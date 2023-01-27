package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendResponse;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.impl.PaperMessagesServiceImpl;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
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
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Mock
    PnAuditLogBuilder auditLogBuilder;

    private PnDeliveryRequest deliveryRequestInProcessing;
    private PnDeliveryRequest deliveryRequestTakingCharge;

    @BeforeEach
    void setUp(){
        this.deliveryRequestInProcessing = getDeliveryRequest("123-adb-567", StatusDeliveryEnum.IN_PROCESSING);
        this.deliveryRequestTakingCharge = getDeliveryRequest("123-cba-572", StatusDeliveryEnum.TAKING_CHARGE);
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

    @Test
    void executionPaperTest() {
        PnAddress address = getPnAddress(deliveryRequestTakingCharge.getRequestId());
        Mockito.when(requestDeliveryDAO.getByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(deliveryRequestTakingCharge));
        Mockito.when(addressDAO.findByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(address));
        Mockito.when(externalChannelClient.sendEngageRequest(Mockito.any())).thenReturn(Mono.just("").then());
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(deliveryRequestTakingCharge));

        Mockito.when(auditLogBuilder.build())
                .thenReturn(new PnAuditLogEvent(PnAuditLogEventType.AUD_FD_SEND, new HashMap<>(), "", new Object()));
        Mockito.when(auditLogBuilder.before(Mockito.any(), Mockito.any()))
                .thenReturn(auditLogBuilder);
        Mockito.when(auditLogBuilder.iun(Mockito.anyString()))
                .thenReturn(auditLogBuilder);

        SendResponse response = paperMessagesService.executionPaper(deliveryRequestTakingCharge.getRequestId(),getRequest(deliveryRequestTakingCharge.getRequestId())).block();
        assertNotNull(response);
        assertNotNull(response.getAmount());
    }

    @Test
    void executionPaperInProcessingTest() {
        Mockito.when(requestDeliveryDAO.getByRequestId(deliveryRequestInProcessing.getRequestId())).thenReturn(Mono.just(deliveryRequestInProcessing));
        StepVerifier.create(paperMessagesService.executionPaper(deliveryRequestInProcessing.getRequestId(),getRequest(deliveryRequestInProcessing.getRequestId())))
                .expectError(PnGenericException.class).verify();
    }

    @Test
    void paperAsyncEntityAndAddressOKTest() {
        PnAddress address = getPnAddress(deliveryRequestTakingCharge.getRequestId());
        Mockito.when(requestDeliveryDAO.getByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(deliveryRequestTakingCharge));
        Mockito.when(addressDAO.findByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(address));
        PaperChannelUpdate response = paperMessagesService.preparePaperSync(deliveryRequestTakingCharge.getRequestId(), getRequestOK()).block();
        assertNotNull(response);
    }

    @Test
    void paperAsyncEntitySecondAttemptTest() {
        PnAddress address = getPnAddress(deliveryRequestTakingCharge.getRequestId());
        Mockito.when(requestDeliveryDAO.getByRequestId(getRelatedRequest().getRelatedRequestId())).thenReturn(Mono.just(deliveryRequestTakingCharge));
        Mockito.when(requestDeliveryDAO.getByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(deliveryRequestTakingCharge));
        Mockito.when(addressDAO.findByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(address));
        PaperChannelUpdate response =paperMessagesService.preparePaperSync(deliveryRequestTakingCharge.getRequestId(), getRelatedRequest()).block();
        assertNotNull(response);
    }


    @Test
    void paperAsyncEntityAndAddressSwitchIfEmptyTest() {
        Mockito.when(auditLogBuilder.build())
                .thenReturn(new PnAuditLogEvent(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, new HashMap<>(), "", new Object()));
        Mockito.when(auditLogBuilder.before(Mockito.any(), Mockito.any()))
                .thenReturn(auditLogBuilder);
        Mockito.when(auditLogBuilder.iun(Mockito.anyString()))
                .thenReturn(auditLogBuilder);

        PnAddress address = getPnAddress(deliveryRequestTakingCharge.getRequestId());
        Mockito.when(requestDeliveryDAO.getByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.empty());
        Mockito.when(addressDAO.findByRequestId(deliveryRequestTakingCharge.getRequestId())).thenReturn(Mono.just(address));
        Mockito.when(requestDeliveryDAO.createWithAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.just(deliveryRequestTakingCharge));
        Mockito.doNothing().when(sqsSender).pushToInternalQueue(Mockito.any());
        StepVerifier.create(paperMessagesService.preparePaperSync(deliveryRequestTakingCharge.getRequestId(), getRequestOK())).expectError(PnPaperEventException.class).verify();
    }

    private SendRequest getRequest(String reqeustId){
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
        analogAddress.setNameRow2("Ettore");

        sendRequest.setRequestId(reqeustId);
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProductType(ProductTypeEnum.RN_AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
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

        sendRequest.setRequestId("123-cba-572");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProposalProductType(ProposalTypeEnum.AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        return sendRequest;
    }

    private PrepareRequest getRelatedRequest(){
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

        sendRequest.setRequestId("123-cba-572");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProposalProductType(ProposalTypeEnum.AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        sendRequest.setRelatedRequestId("123abcd1234");
        sendRequest.setDiscoveredAddress(analogAddress);
        return sendRequest;
    }

    private PnDeliveryRequest getDeliveryRequest(String requestId, StatusDeliveryEnum status){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
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
        address.setFromNationalRegistry(true);

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
