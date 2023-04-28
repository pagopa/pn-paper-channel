package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.BaseTest;
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
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressOKDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.service.impl.PaperMessagesServiceImpl;
import it.pagopa.pn.paperchannel.utils.Utility;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.validator.SendRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
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
    private SqsSender sqsSender;

    @SpyBean
    PnAuditLogBuilder auditLogBuilder;


    private PnDeliveryRequest deliveryRequestTakingCharge;

    private MockedStatic<SendRequestValidator> sendRequestValidatorMockedStatic;
    private MockedStatic<PrepareRequestValidator> prepareRequestValidatorMockedStatic;


    @BeforeEach
    void setUp(){
        this.sendRequestValidatorMockedStatic = Mockito.mockStatic(SendRequestValidator.class);
        this.prepareRequestValidatorMockedStatic = Mockito.mockStatic(PrepareRequestValidator.class);
        this.deliveryRequestTakingCharge = getDeliveryRequest(getRequestOK().getRequestId(), StatusDeliveryEnum.TAKING_CHARGE);
    }

    @AfterEach
    void afterEach(){
        this.sendRequestValidatorMockedStatic.close();
        this.prepareRequestValidatorMockedStatic.close();
    }

    /**
     * PREAPARE PAPER TEST WITH METHOD GET
     */
    @Test
    @DisplayName("whenRetrievePaperDeliveryRequestNotExistThenThrowError")
    void retrievePrepareRequestNotExist(){
        Mockito.when(this.requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperMessagesService.retrievePaperPrepareRequest("TST-IOR.2332"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();

    }

    //@Test
    //@DisplayName("whenRetrievePaperDeliveryRequestExistThenReturnResponse")
    void retrievePrepareRequestExist(){
        PnDeliveryRequest deliveryRequest = getPnDeliveryRequest();
        Mockito.when(this.requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.addressDAO.findByRequestId(Mockito.any()))
                .thenReturn(Mono.just(getPnAddress(deliveryRequest.getRequestId())));

        PrepareEvent event = this.paperMessagesService.retrievePaperPrepareRequest("TST-IOR.2332").block();
        assertNotNull(event);
        assertEquals(deliveryRequest.getRequestId(), event.getRequestId());
    }


    /**
     * PREAPARE PAPER TEST WITH METHOD POST
     */
    @Test
    @DisplayName("whenPrepareFirstAttemptWithDeliveryRequestNotExistThenStartAsyncFlow")
    void prepareSyncDeliveryRequestNotExistFirstAttempt(){
        Mockito.when(this.requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(requestDeliveryDAO.createWithAddress(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getPnDeliveryRequest()));

        Mockito.doNothing().when(this.sqsSender).pushToInternalQueue(Mockito.any());

        StepVerifier.create(this.paperMessagesService.preparePaperSync("TST-IOR.2332", getRequestOK()))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnPaperEventException);
                    return true;
                }).verify();
    }

    //@Test
    //@DisplayName("whenPrepareFirstAttemptWithDeliveryRequestExistThenReturnResponse")
    void prepareSyncDeliveryRequestExistFirstAttempt(){
        Mockito.when(this.requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.just(getPnDeliveryRequest()));

        prepareRequestValidatorMockedStatic.when(() -> {
            PrepareRequestValidator.compareRequestEntity(getRequestOK(), getPnDeliveryRequest(), true);
        }).thenAnswer((Answer<Void>) invocation -> null);

        Mockito.when(this.addressDAO.findByRequestId(Mockito.any()))
                .thenReturn(Mono.just(getPnAddress(getPnDeliveryRequest().getRequestId())));

        PaperChannelUpdate update = this.paperMessagesService.preparePaperSync("TST-IOR.2332", getRequestOK()).block();

        assertNotNull(update);
        assertNotNull(update.getPrepareEvent());
        assertNull(update.getSendEvent());

    }

    @Test
    @DisplayName("whenPrepareSecondAttemptWithOldRequestNotExistedThrowError")
    void prepareSyncSecondAttemptRelatedRequestNotExisted(){

        //ADDED RELATED REQUEST ID FOR SECOND ATTEMPT
        //ADDED DISCOVERED ADDRESS FOR START ASYNC FLOW AND NOT NATIONAL REGISTRY
        PrepareRequest request = getRequestOK();
        request.setRelatedRequestId("ABS-1234");
        request.setDiscoveredAddress(getAnalogAddress());

        //MOCK RELATED DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId(request.getRelatedRequestId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperMessagesService.preparePaperSync("TST-IOR.2332", request))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();
    }


    @Test
    @DisplayName("whenPrepareSecondAttemptWithErrorValidationOldDeliveryRequest")
    void prepareSyncSecondAttemptErrorValidation(){
        PnDeliveryRequest deliveryRequest = getPnDeliveryRequest();
        deliveryRequest.setRelatedRequestId("ABS-1234");

        //ADDED RELATED REQUEST ID FOR SECOND ATTEMPT
        //ADDED DISCOVERED ADDRESS FOR START ASYNC FLOW AND NOT NATIONAL REGISTRY
        PrepareRequest request = getRequestOK();
        request.setRelatedRequestId("ABS-1234");
        request.setDiscoveredAddress(getAnalogAddress());

        //MOCK RELATED DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId(request.getRelatedRequestId()))
                .thenReturn(Mono.just(deliveryRequest));

        // MOCK ERROR VALIDATION OLD REQUEST
        prepareRequestValidatorMockedStatic.when(() -> {
            PrepareRequestValidator.compareRequestEntity(
                    request, deliveryRequest, false);
        }).thenThrow(new PnInputValidatorException(DIFFERENT_DATA_REQUEST,DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, null));


        StepVerifier.create(this.paperMessagesService.preparePaperSync("TST-IOR.2332", request))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnInputValidatorException);
                    assertEquals(DIFFERENT_DATA_REQUEST,((PnInputValidatorException) ex).getExceptionType());
                    assertEquals(HttpStatus.CONFLICT, ((PnInputValidatorException) ex).getHttpStatus());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenPrepareSecondAttemptWithNewRequestNotExistedThenCallAsyncFlow")
    void prepareSyncSecondAttemptAsyncFlow(){

        PnDeliveryRequest deliveryRequest = getPnDeliveryRequest();
        deliveryRequest.setRelatedRequestId("ABS-1234");

        //ADDED RELATED REQUEST ID FOR SECOND ATTEMPT
        //ADDED DISCOVERED ADDRESS FOR START ASYNC FLOW AND NOT NATIONAL REGISTRY
        PrepareRequest request = getRequestOK();
        request.setRelatedRequestId("ABS-1234");
        request.setDiscoveredAddress(getAnalogAddress());

        //MOCK RELATED DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId(request.getRelatedRequestId()))
                .thenReturn(Mono.just(deliveryRequest));

        //MOCK VALIDATION
        prepareRequestValidatorMockedStatic.when(() -> {
            PrepareRequestValidator.compareRequestEntity(request, deliveryRequest, false);
        }).thenAnswer((Answer<Void>) invocation -> null);

        //MOCK NEW DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                        .thenReturn(Mono.empty());

        //MOCK OLD ADDRESS GET
        Mockito.when(addressDAO.findByRequestId(Mockito.any()))
                        .thenReturn(Mono.just(getPnAddress("OLD_ADDRESS")));

        //MOCK SAVE NEW DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.createWithAddress(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        //MOCK PUSH QUEUE
        Mockito.doNothing().when(this.sqsSender).pushToInternalQueue(Mockito.any());

        StepVerifier.create(this.paperMessagesService.preparePaperSync("TST-IOR.2332", request))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnPaperEventException);
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenPrepareSecondAttemptWithNewRequestNotExistedThenNationalRegistryFlow")
    void prepareSyncSecondAttemptNationalRegistryFlow(){

        PnDeliveryRequest deliveryRequest = getPnDeliveryRequest();
        deliveryRequest.setRelatedRequestId("ABS-1234");

        //ADDED RELATED REQUEST ID FOR SECOND ATTEMPT
        //ADDED DISCOVERED ADDRESS FOR START ASYNC FLOW AND NOT NATIONAL REGISTRY
        PrepareRequest request = getRequestOK();
        request.setRelatedRequestId("ABS-1234");
        request.setDiscoveredAddress(null);

        //MOCK RELATED DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId(request.getRelatedRequestId()))
                .thenReturn(Mono.just(deliveryRequest));

        //MOCK VALIDATION
        prepareRequestValidatorMockedStatic.when(() -> {
            PrepareRequestValidator.compareRequestEntity(request, deliveryRequest, false);
        }).thenAnswer((Answer<Void>) invocation -> null);

        //MOCK NEW DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.getByRequestId("TST-IOR.2332"))
                .thenReturn(Mono.empty());

        //MOCK OLD ADDRESS GET
        Mockito.when(addressDAO.findByRequestId(Mockito.any()))
                .thenReturn(Mono.just(getPnAddress("OLD_ADDRESS")));

        //MOCK SAVE NEW DELIVERY REQUEST
        Mockito.when(requestDeliveryDAO.createWithAddress(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.nationalRegistryClient.finderAddress(Mockito.any(), Mockito.any(), Mockito.any()))
                        .thenReturn(Mono.just(new AddressOKDto()));

        StepVerifier.create(this.paperMessagesService.preparePaperSync("TST-IOR.2332", request))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnPaperEventException);
                    return true;
                }).verify();
    }


    /**
     * EXECUTION PAPER TEST WITH METHOD GET
     */

    @Test
    @DisplayName("whenRetrieveRequestDeliveryNotExistedThenThrowNotExist")
    void paperSendRequestNoRequestDelivery(){
        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperMessagesService.retrievePaperSendRequest("TST-IOR.2332"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenRetrieveRequestWithIncongruentStatusCode")
    void paperSendRequestIncongruentStatusCode(){
        PnDeliveryRequest deliveryRequest = getPnDeliveryRequest();

        // WITH STATUS TAKING_CHARGE

        deliveryRequest.setStatusCode(StatusDeliveryEnum.TAKING_CHARGE.getCode());

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any()))
                        .thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(this.paperMessagesService.retrievePaperSendRequest("TST-IOR.2332"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();

        // WITH STATUS IN_PROCESSING

        deliveryRequest.setStatusCode(StatusDeliveryEnum.IN_PROCESSING.getCode());

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(this.paperMessagesService.retrievePaperSendRequest("TST-IOR.2332"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();

        // WITH STATUS PAPER_CHANNEL_DEFAULT_ERROR

        deliveryRequest.setStatusCode(StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR.getCode());

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(this.paperMessagesService.retrievePaperSendRequest("TST-IOR.2332"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();

        // WITH STATUS PAPER_CHANNEL_NEW_REQUEST

        deliveryRequest.setStatusCode(StatusDeliveryEnum.PAPER_CHANNEL_NEW_REQUEST.getCode());

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(this.paperMessagesService.retrievePaperSendRequest("TST-IOR.2332"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST,((PnGenericException) ex).getExceptionType());
                    assertEquals(HttpStatus.NOT_FOUND, ((PnGenericException) ex).getHttpStatus());
                    return true;
                }).verify();

    }


    @Test
    @DisplayName("whenRetrieveRequestWithCorrectStatusCodeThenReturnResponse")
    void paperSendRequestCorrectStatusCode(){
        PnDeliveryRequest deliveryRequest = getPnDeliveryRequest();

        deliveryRequest.setStatusCode(StatusDeliveryEnum.READY_TO_SEND.getCode());
        deliveryRequest.setStatusDetail(StatusDeliveryEnum.READY_TO_SEND.getDescription());

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        Mockito.when(addressDAO.findByRequestId(Mockito.any()))
                .thenReturn(Mono.just(getPnAddress(deliveryRequest.getRequestId())));

        SendEvent sendEvent = this.paperMessagesService.retrievePaperSendRequest("TST-IOR.2332").block();

        assertNotNull(sendEvent);
        assertEquals(deliveryRequest.getRequestId(), sendEvent.getRequestId());
        //assertEquals(deliveryRequest.getStatusCode(), sendEvent.getStatusCode().getValue());
        assertEquals(deliveryRequest.getProductType(), sendEvent.getRegisteredLetterCode());

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

        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNationalCost()));

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

        //MOCK RETRIEVE INTERNATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getInternationalCost()));

        StepVerifier.create(paperMessagesService.executionPaper("TST-IOR.2332", sendRequest))
                .expectNextMatches((response) -> {
                    // price 1 and additionalPrice 2 getNationalCost()
                    // attachments 1 and number of page 3
                    assertEquals(800,response.getAmount());
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
                    assertEquals(3, response.getNumberOfPages());
                    return true;
                }).verifyComplete();

    }

    //@Test
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
