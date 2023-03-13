package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
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
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.service.impl.PaperMessagesServiceImpl;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
import it.pagopa.pn.paperchannel.utils.Utility;
import it.pagopa.pn.paperchannel.validator.SendRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class PaperMessagesServiceTest extends BaseTest {

    @Autowired
    private PaperMessagesServiceImpl paperMessagesService;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private AddressDAO addressDAO;
    @MockBean
    private PaperTenderService paperTenderService;

    @MockBean
    private NationalRegistryClient nationalRegistryClient;

    @MockBean
    private ExternalChannelClient externalChannelClient;

    @MockBean
    private PrepareAsyncServiceImpl prepareAsyncService;

    @MockBean
    private SqsSender sqsSender;

    @SpyBean
    PnAuditLogBuilder auditLogBuilder;

    @SpyBean
    private PnPaperChannelConfig paperChannelConfig;

    private PnDeliveryRequest deliveryRequestInProcessing;
    private PnDeliveryRequest deliveryRequestTakingCharge;

    private MockedStatic<SendRequestValidator> sendRequestValidatorMockedStatic;


    @BeforeEach
    void setUp(){
        this.sendRequestValidatorMockedStatic = Mockito.mockStatic(SendRequestValidator.class);
        this.deliveryRequestInProcessing = getDeliveryRequest("123-adb-567", StatusDeliveryEnum.IN_PROCESSING);
        this.deliveryRequestTakingCharge = getDeliveryRequest(getRequestOK().getRequestId(), StatusDeliveryEnum.TAKING_CHARGE);
    }

    @AfterEach
    void afterEach(){
        this.sendRequestValidatorMockedStatic.close();
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
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(getPnDeliveryRequest()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress("abcde12345")));
        PrepareEvent prepareEvent = paperMessagesService.retrievePaperPrepareRequest("abcde12345").block();
        assertNotNull(prepareEvent);
        Assertions.assertEquals(StatusCodeEnum.PROGRESS, prepareEvent.getStatusCode());
    }

    @Test
    void paperMessagesServiceTest2() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.just(gePnDeliveryRequestUntraceable()));
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress("abcde12345")));
        PrepareEvent prepareEvent = paperMessagesService.retrievePaperPrepareRequest("abcde12345").block();
        assertNotNull(prepareEvent);
        Assertions.assertEquals(StatusCodeEnum.KOUNREACHABLE, prepareEvent.getStatusCode());
    }

    @Test
    void paperMessagesServiceTestErrorRequest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("abcde12345")).thenReturn(Mono.empty());
        Mockito.when(addressDAO.findByRequestId("abcde12345")).thenReturn(Mono.just(getPnAddress("abcde12345")));
        StepVerifier.create(paperMessagesService.retrievePaperPrepareRequest("abcde12345")).expectError(PnGenericException.class).verify();
    }

    /**
     * EXECUTION PAPER TEST WITH METHOD POST
     */
    @Test
    void executionPaperRequestDeliveryNotFoundTest() {
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332")).thenReturn(Mono.empty());
        StepVerifier.create(this.paperMessagesService.executionPaper("TST-IOR.2332", new SendRequest()))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();

    }

    @Test
    void executionPaperValidationThrowError() {
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                .thenReturn(Mono.just(getPnDeliveryRequest()));

        sendRequestValidatorMockedStatic.when(() -> {
            SendRequestValidator.compareRequestEntity(Mockito.any(), Mockito.any());
        }).thenThrow(new PnInputValidatorException(DIFFERENT_DATA_REQUEST,DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, null));

        StepVerifier.create(this.paperMessagesService.executionPaper("TST-IOR.2332", new SendRequest()))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnInputValidatorException);
                    assertEquals(DIFFERENT_DATA_REQUEST,((PnInputValidatorException) ex).getExceptionType());
                    assertEquals(HttpStatus.CONFLICT, ((PnInputValidatorException) ex).getHttpStatus());
                    return true;
                }).verify();
    }

    @Test
    void executionPaperThrowErrorWhenStatusRequestIdInProcessing() {
        PnDeliveryRequest request = getPnDeliveryRequest();
        request.setStatusCode(StatusDeliveryEnum.IN_PROCESSING.getCode());
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                .thenReturn(Mono.just(request));

        sendRequestValidatorMockedStatic.when(() -> {
            SendRequestValidator.compareRequestEntity(Mockito.any(), Mockito.any());
        }).thenAnswer((Answer<Void>) invocation -> null);



        StepVerifier.create(this.paperMessagesService.executionPaper("TST-IOR.2332", new SendRequest()))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_IN_PROCESSING,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.CONFLICT, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();
    }

    @Test
    void executionPaperWithStatusTakingChargeTest() {
        PnDeliveryRequest request = getPnDeliveryRequest();
        request.setStatusCode(StatusDeliveryEnum.TAKING_CHARGE.getCode());

        SendRequest sendRequest = getRequest("TST-IOR.2332");
        sendRequest.setRequestPaId("request-pad-id");
        sendRequest.setPrintType("FRONTE-RETRO");

        //MOCK GET DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                .thenReturn(Mono.just(request));

        //MOCK VALIDATOR
        sendRequestValidatorMockedStatic.when(() -> {
            SendRequestValidator.compareRequestEntity(Mockito.any(), Mockito.any());
        }).thenAnswer((Answer<Void>) invocation -> null);

        //MOCK ALL CREATE ADDRESS
        Mockito.when(addressDAO.create(Mockito.any())).thenReturn(Mono.just(new PnAddress()));

        //MOCK SEND ENGAGE EXTERNAL CHANNEL
        Mockito.when(externalChannelClient.sendEngageRequest(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just("").then());

        //MOCK UPDATE DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(request));

        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNationalCost()));

        StepVerifier.create(paperMessagesService.executionPaper("TST-IOR.2332", sendRequest))
                .expectNextMatches((response) -> {
                    // price 1 and additionalPrice 2 getNationalCost()
                    // attachments 1 and number of page 3
                    assertEquals(700,response.getAmount());
                    assertEquals(sendRequest.getReceiverAddress().getCap(), response.getZip());
                    assertEquals(3, response.getNumberOfPages());
                    return true;
                }).verifyComplete();

    }

    @Test
    void executionPaperWhenExternalChannelThrowErrorTest() {
        PnDeliveryRequest request = getPnDeliveryRequest();
        request.setStatusCode(StatusDeliveryEnum.TAKING_CHARGE.getCode());

        SendRequest sendRequest = getRequest("TST-IOR.2332");
        sendRequest.setRequestPaId("request-pad-id");
        sendRequest.setPrintType("FRONTE-RETRO");

        //MOCK GET DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                .thenReturn(Mono.just(request));

        //MOCK VALIDATOR
        sendRequestValidatorMockedStatic.when(() -> {
            SendRequestValidator.compareRequestEntity(Mockito.any(), Mockito.any());
        }).thenAnswer((Answer<Void>) invocation -> null);

        //MOCK ALL CREATE ADDRESS
        Mockito.when(addressDAO.create(Mockito.any())).thenReturn(Mono.just(new PnAddress()));

        //MOCK SEND ENGAGE EXTERNAL CHANNEL
        Mockito.when(externalChannelClient.sendEngageRequest(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new PnGenericException(EXTERNAL_CHANNEL_API_EXCEPTION, EXTERNAL_CHANNEL_API_EXCEPTION.getMessage())));



        StepVerifier.create(paperMessagesService.executionPaper("TST-IOR.2332", sendRequest))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(EXTERNAL_CHANNEL_API_EXCEPTION,((PnGenericException) ex).getExceptionType());
                    return true;
                }).verify();

    }

    @Test
    void executionPaperWithInternationalCostTest() {
        PnDeliveryRequest request = getPnDeliveryRequest();
        request.setStatusCode(StatusDeliveryEnum.TAKING_CHARGE.getCode());
        request.setProductType(ProductTypeEnum.AR.getValue());

        SendRequest sendRequest = getRequest("TST-IOR.2332");
        sendRequest.setRequestPaId("request-pad-id");
        sendRequest.setPrintType("FRONTE-RETRO");
        sendRequest.getReceiverAddress().setCap(null);
        sendRequest.getReceiverAddress().setCountry("GERMANY");
        sendRequest.setProductType(ProductTypeEnum.RIR);
        sendRequest.setArAddress(getAnalogAddress());
        sendRequest.setSenderAddress(getAnalogAddress());

        //MOCK GET DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                .thenReturn(Mono.just(request));

        //MOCK VALIDATOR
        sendRequestValidatorMockedStatic.when(() -> {
            SendRequestValidator.compareRequestEntity(Mockito.any(), Mockito.any());
        }).thenAnswer((Answer<Void>) invocation -> null);

        //MOCK ALL CREATE ADDRESS
        Mockito.when(addressDAO.create(Mockito.any())).thenReturn(Mono.just(new PnAddress()));

        //MOCK SEND ENGAGE EXTERNAL CHANNEL
        Mockito.when(externalChannelClient.sendEngageRequest(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just("").then());

        //MOCK UPDATE DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(request));

        //MOCK RETRIEVE ZONE FROM COUNTRY
        Mockito.when(paperTenderService.getZoneFromCountry(Mockito.any()))
                .thenReturn(Mono.just("ZONE_1"));

        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getInternationalCost()));

        StepVerifier.create(paperMessagesService.executionPaper("TST-IOR.2332", sendRequest))
                .expectNextMatches((response) -> {
                    // price 1 and additionalPrice 2 getNationalCost()
                    // attachments 1 and number of page 3
                    assertEquals(800,response.getAmount());
                    assertNull(response.getZip());
                    assertEquals(sendRequest.getReceiverAddress().getCountry(), response.getForeignState());
                    assertEquals(3, response.getNumberOfPages());
                    return true;
                }).verifyComplete();

    }

    @Test
    void executionPaperWithAlreadyStatusReadyToSendTest() {
        PnDeliveryRequest request = getPnDeliveryRequest();
        request.setStatusCode(StatusDeliveryEnum.READY_TO_SEND.getCode());
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                .thenReturn(Mono.just(request));

        sendRequestValidatorMockedStatic.when(() -> {
            SendRequestValidator.compareRequestEntity(Mockito.any(), Mockito.any());
        }).thenAnswer((Answer<Void>) invocation -> null);

        Mockito.when(addressDAO.create(Mockito.any())).thenReturn(Mono.just(new PnAddress()));
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNationalCost()));

        StepVerifier.create(paperMessagesService.executionPaper("TST-IOR.2332", getRequest("TST-IOR.2332")))
                .expectNextMatches((response) -> {
                    // price 1 and additionalPrice 2 getNationalCost()
                    // attachments 1 and number of page 3
                    assertEquals(700,response.getAmount());
                    assertEquals(getRequest("").getReceiverAddress().getCap(), response.getZip());
                    assertEquals(3, response.getNumberOfPages());
                    return true;
                }).verifyComplete();

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

    private SendRequest getRequest(String requestId){
        SendRequest sendRequest= new SendRequest();
        List<String> attachmentUrls = new ArrayList<>();
        String s = "http://localhost:8080";
        attachmentUrls.add(s);

        sendRequest.setRequestId(requestId);
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProductType(ProductTypeEnum.AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);

        sendRequest.setReceiverAddress(getAnalogAddress());
        return sendRequest;
    }
    private PnDeliveryRequest getPnDeliveryRequest(){
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

        deliveryRequest.setAddressHash(getAddress().convertToHash());
        deliveryRequest.setRequestId("12345abcde");
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("RT");
        deliveryRequest.setIun("");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode("PC000");
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setHashedFiscalCode(Utility.convertToHash(deliveryRequest.getFiscalCode()));
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("AR");
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

        deliveryRequest.setAddressHash(getAddress().convertToHash());
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
        deliveryRequest.setProductType("AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }
    private PrepareRequest getRequestOK(){
        PrepareRequest sendRequest= new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        String s = "http://localhost:8080";
        attachmentUrls.add(s);


        sendRequest.setRequestId("123-cba-572");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProposalProductType(ProposalTypeEnum.AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(getAnalogAddress());
        return sendRequest;
    }
    private PrepareRequest getRelatedRequest(){
        PrepareRequest sendRequest= new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        String s = "http://localhost:8080";
        attachmentUrls.add(s);

        sendRequest.setRequestId("123-cba-572");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProposalProductType(ProposalTypeEnum.AR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(getAnalogAddress());
        sendRequest.setRelatedRequestId("123abcd1234");
        sendRequest.setDiscoveredAddress(getAnalogAddress());
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


        deliveryRequest.setAddressHash(getAddress().convertToHash());
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
        deliveryRequest.setHashedFiscalCode(Utility.convertToHash(deliveryRequest.getFiscalCode()));
        deliveryRequest.setProductType("AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }

    private CostDTO getNationalCost() {
        CostDTO dto = new CostDTO();
        dto.setPrice(1.00F);
        dto.setPriceAdditional(2.00F);
        return dto;
    }

    private CostDTO getInternationalCost() {
        CostDTO dto = new CostDTO();
        dto.setPrice(2.00F);
        dto.setPriceAdditional(2.00F);
        return dto;
    }

    private AnalogAddress getAnalogAddress(){
        AnalogAddress address = new AnalogAddress();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setNameRow2("Ettore");
        return address;
    }

    private Address getAddress(){
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
        return address;
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


}
