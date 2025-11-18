package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ADDRESS_MANAGER_ERROR;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ATTEMPT_ADDRESS_NATIONAL_REGISTRY;
import static it.pagopa.pn.paperchannel.utils.Const.RACCOMANDATA_SEMPLICE;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreparePhaseOneAsyncServiceImplTest {

    @InjectMocks
    private PreparePhaseOneAsyncServiceImpl preparePhaseOneAsyncService;
    
    @Mock
    private PaperCalculatorUtils paperCalculatorUtils;

    @Mock
    private PaperAddressService paperAddressService;

    @Mock
    private AddressDAO addressDAO;

    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;

    @Mock
    private PrepareFlowStarter prepareFlowStarter;

    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @Mock
    private PnPaperChannelConfig config;

    @Mock
    private PaperTenderService paperTenderService;

    @Mock
    private PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;

    @Mock
    private SqsSender sqsSender;



    private final PrepareNormalizeAddressEvent request = new PrepareNormalizeAddressEvent();

    private final PnAttachmentInfo attachmentInfo = new PnAttachmentInfo();

    @BeforeEach
    public void setUp(){
        inizialize();
    }

    @Test
    void preparePhaseOneAsyncAttemptZeroTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest =  getDeliveryRequest(requestId, iun);
        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var address = getAddress();

        PnAddress addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);

        var cost = new PnPaperChannelCostDTO();
        cost.setTenderId("TENDER_ID");
        cost.setDeliveryDriverId("DRIVER_ID");

        var driver = new PaperChannelDeliveryDriver();
        driver.setUnifiedDeliveryDriver("UNIFIED_DRIVER");

        when(requestDeliveryDAO.getByRequestId(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, null, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(paperTenderService.getSimplifiedCost(address.getCap(), deliveryRequest.getProductType())).thenReturn(Mono.just(cost));
        when(paperChannelDeliveryDriverDAO.getByDeliveryDriverId("DRIVER_ID")).thenReturn(Mono.just(driver));
        when(requestDeliveryDAO.updateDataWithoutGet(deliveryRequest, false)).thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        //verifico che viene inviato l'evento di output della PREPARE fase 1
        verify(prepareFlowStarter, times(1)).pushPreparePhaseOneOutput(deliveryRequest, addressEntity, "UNIFIED_DRIVER");
        verify(prepareFlowStarter, never()).pushResultPrepareEvent(any(), any(), any(), any(), any());
    }

    @Test
    void preparePhaseOneAsyncAttemptOneTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest = getDeliveryRequest(requestId, iun);
        var address = getAddress();

        var cost = new PnPaperChannelCostDTO();
        cost.setTenderId("TENDER_ID");
        cost.setDeliveryDriverId("DRIVER_ID");

        var driver = new PaperChannelDeliveryDriver();
        driver.setUnifiedDeliveryDriver("UNIFIED_DRIVER");

        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId(requestId)
                .iun(iun)
                .address(address)
                .attempt(0)
                .build();

        PnAddress addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);


        when(requestDeliveryDAO.getByRequestId(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, address, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(paperTenderService.getSimplifiedCost(address.getCap(), deliveryRequest.getProductType())).thenReturn(Mono.just(cost));
        when(paperChannelDeliveryDriverDAO.getByDeliveryDriverId("DRIVER_ID")).thenReturn(Mono.just(driver));
        when(requestDeliveryDAO.updateDataWithoutGet(deliveryRequest, false)).thenReturn(Mono.just(deliveryRequest));


        StepVerifier.create(preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        //verifico che viene inviato l'evento di output della PREPARE fase 1
        verify(prepareFlowStarter, times(1)).pushPreparePhaseOneOutput(deliveryRequest, addressEntity, "UNIFIED_DRIVER");
        verify(prepareFlowStarter, never()).pushResultPrepareEvent(any(), any(), any(), any(), any());
    }

    @Test
    void preparePhaseOneAsyncTestErrorUntraceableAddress(){
        PnDeliveryRequest deliveryRequest = getDeliveryRequest("FATY-FATY-2023041520230302", "FATY-FATY-2023041520230302-101111");
        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.UNTRACEABLE;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();
        KOReason koReason = new KOReason(FailureDetailCodeEnum.D00, null);

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);

        when(this.requestDeliveryDAO.getByRequestId(any(), anyBoolean()))
                .thenReturn(Mono.just(deliveryRequest));

        when(this.paperAddressService.getCorrectAddress(any(), any(), anyInt()))
                .thenReturn(Mono.error(new PnUntracebleException(koReason)));

        when(this.requestDeliveryDAO.updateStatus(eq(deliveryRequest.getRequestId()), eq(statusCode), eq(statusDescription), eq(statusDetail), any())).thenReturn(Mono.empty());

        doNothing().when(this.prepareFlowStarter).pushResultPrepareEvent(eq(deliveryRequest), isNull(),eq("clientId"), eq(StatusCodeEnum.KO), eq(koReason));

        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("FATY-FATY-2023041520230302")
                .clientId("clientId")
                .iun("FATY-FATY-2023041520230302-101111")
                .attempt(0)
                .build();

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(PnGenericException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, times(1)).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), eq(koReason));
        verify(prepareFlowStarter, never()).pushPreparePhaseOneOutput(any(), any(), any());
        verify(requestDeliveryDAO,times(1)).updateStatus(eq(deliveryRequest.getRequestId()), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
    }

    @Test
    void preparePhaseOneAsyncSimplifiedCostTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest = getDeliveryRequest(requestId, iun);
        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var address = getAddress();

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("Generic Error");
        pnRequestError.setGeokey("geokey");
        pnRequestError.setFlowThrow("PREPARE_PHASE_ONE_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##" + Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("FATY-FATY-2023041520230302");
        pnRequestError.setCreated(Instant.now());

        RuntimeException runtimeException = new RuntimeException("Generic Error");

        PnAddress addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);

        when(requestDeliveryDAO.getByRequestId(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, null, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(requestDeliveryDAO.updateDataWithoutGet(deliveryRequest, false)).thenReturn(Mono.just(deliveryRequest));
        when(paperTenderService.getSimplifiedCost(address.getCap(), deliveryRequest.getProductType())).thenReturn(Mono.error(runtimeException));
        when(requestDeliveryDAO.updateStatus(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(RuntimeException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, never()).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), isNull());
        verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        assertionItemCapturedWithItemError(itemErrorCaptor, pnRequestError);

        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
    }

    @Test
    void preparePhaseOneAsyncFilterAttachmentTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest =  getDeliveryRequest(requestId, iun);
        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var address = getAddress();

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("Generic error");
        pnRequestError.setGeokey("geokey");
        pnRequestError.setFlowThrow("PREPARE_PHASE_ONE_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("FATY-FATY-2023041520230302");
        pnRequestError.setCreated(Instant.now());

        RuntimeException runtimeException = new RuntimeException("Generic error");

        PnAddress addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);

        var cost = new PnPaperChannelCostDTO();
        cost.setTenderId("TENDER_ID");
        cost.setDeliveryDriverId("DRIVER_ID");

        var driver = new PaperChannelDeliveryDriver();
        driver.setUnifiedDeliveryDriver("UNIFIED_DRIVER");

        when(requestDeliveryDAO.getByRequestId(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, null, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(requestDeliveryDAO.updateStatus(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(RuntimeException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, never()).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), isNull());
        verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        assertionItemCapturedWithItemError(itemErrorCaptor, pnRequestError);

        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
    }

    @Test
    void preparePhaseOneAsyncFilterAttachmentNotificationSentAtNullTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest =  getDeliveryRequest(requestId, iun);
        deliveryRequest.setNotificationSentAt(null); //per test nullpointer bug
        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var address = getAddress();

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("is null");
        pnRequestError.setGeokey("geokey");
        pnRequestError.setFlowThrow("PREPARE_PHASE_ONE_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("FATY-FATY-2023041520230302");
        pnRequestError.setCreated(Instant.now());

        NullPointerException nullPointerException = new NullPointerException("is null");

        PnAddress addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);

        var cost = new PnPaperChannelCostDTO();
        cost.setTenderId("TENDER_ID");
        cost.setDeliveryDriverId("DRIVER_ID");

        var driver = new PaperChannelDeliveryDriver();
        driver.setUnifiedDeliveryDriver("UNIFIED_DRIVER");

        when(requestDeliveryDAO.getByRequestId(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, null, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(requestDeliveryDAO.updateStatus(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(NullPointerException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, never()).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), isNull());
        verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        assertionItemCapturedWithItemError(itemErrorCaptor, pnRequestError);

        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());

    }

    private void assertionItemCapturedWithItemError(ArgumentCaptor<PnRequestError> itemErrorCaptor, PnRequestError pnRequestError) {
        PnRequestError pnRequestErrorForAssertion = itemErrorCaptor.getValue();
        Assertions.assertEquals(pnRequestError.getError(),pnRequestErrorForAssertion.getError());
        Assertions.assertEquals(pnRequestError.getFlowThrow(),pnRequestErrorForAssertion.getFlowThrow());
        Assertions.assertEquals(pnRequestError.getCategory(),pnRequestErrorForAssertion.getCategory());
    }

    @Test
    void preparePhaseOneAsyncTestCheckAddressError(){
        PnDeliveryRequest deliveryRequest = getDeliveryRequest("FATY-FATY-2023041520230302", "FATY-FATY-2023041520230302-101111");
        String requestId = deliveryRequest.getRequestId();
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("Problemi con l'indirizzo");
        pnRequestError.setGeokey("geokey");
        pnRequestError.setFlowThrow("CHECK_ADDRESS_FLOW");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("FATY-FATY-2023041520230302");
        pnRequestError.setCreated(Instant.now());

        StopFlowSecondAttemptException stopFlowSecondAttemptException = new StopFlowSecondAttemptException(ADDRESS_MANAGER_ERROR,ADDRESS_MANAGER_ERROR.getMessage(), "geokey");
        CheckAddressFlowException checkAddressFlowException = new CheckAddressFlowException(stopFlowSecondAttemptException, "geokey");

        when(this.requestDeliveryDAO.getByRequestId(any(), anyBoolean()))
                .thenReturn(Mono.just(deliveryRequest));

        when(this.paperAddressService.getCorrectAddress(any(), any(), anyInt()))
                .thenReturn(Mono.error(checkAddressFlowException));
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));
        when(requestDeliveryDAO.updateStatus(eq(requestId), eq(statusCode), eq(statusDescription), eq(statusDetail), any())).thenReturn(Mono.empty());

        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("FATY-FATY-2023041520230302")
                .clientId("clientId")
                .iun("FATY-FATY-2023041520230302-101111")
                .attempt(0)
                .build();

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(CheckAddressFlowException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, never()).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), isNull());
        verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        assertionItemCapturedWithItemError(itemErrorCaptor, pnRequestError);

        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
    }

    @Test
    void preparePhaseOneAsyncTestErrorPnAddressFlowPostmanFlow(){
        PnDeliveryRequest deliveryRequest = getDeliveryRequest("FATY-FATY-2023041520230302", "FATY-FATY-2023041520230302-101111");

        when(this.requestDeliveryDAO.getByRequestId(any(), anyBoolean()))
                .thenReturn(Mono.just(deliveryRequest));

        when(this.paperAddressService.getCorrectAddress(any(), any(), anyInt()))
                .thenReturn(Mono.error(new PnAddressFlowException(ATTEMPT_ADDRESS_NATIONAL_REGISTRY)));

        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("FATY-FATY-2023041520230302")
                .clientId("clientId")
                .iun("FATY-FATY-2023041520230302-101111")
                .attempt(0)
                .build();

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(PnAddressFlowException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, never()).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), isNull());
        verify(prepareFlowStarter, never()).pushPreparePhaseOneOutput(any(), any(), any());
        verify(requestDeliveryDAO,never()).updateStatus(eq(deliveryRequest.getRequestId()), any(), any(), any(), any());
    }
    @Test
    void preparePhaseOneAsyncTestPnAddresManagerError(){
        PnDeliveryRequest deliveryRequest = getDeliveryRequest("FATY-FATY-2023041520230302", "FATY-FATY-2023041520230302-101111");

        when(this.requestDeliveryDAO.getByRequestId(any(), anyBoolean()))
                .thenReturn(Mono.just(deliveryRequest));

        when(this.paperAddressService.getCorrectAddress(any(), any(), anyInt()))
                .thenReturn(Mono.error(new PnAddressFlowException(ADDRESS_MANAGER_ERROR)));

        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("FATY-FATY-2023041520230302")
                .clientId("clientId")
                .iun("FATY-FATY-2023041520230302-101111")
                .attempt(0)
                .build();

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(PnAddressFlowException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, never()).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), isNull());
        verify(prepareFlowStarter, never()).pushPreparePhaseOneOutput(any(), any(), any());
        verify(requestDeliveryDAO,never()).updateStatus(eq(deliveryRequest.getRequestId()), any(), any(), any(), any());
    }
    @Test
    void preparePhaseOneAsyncTestGetRequestError(){
        PnDeliveryRequest deliveryRequest = getDeliveryRequest("FATY-FATY-2023041520230302", "FATY-FATY-2023041520230302-101111");
        String requestId = deliveryRequest.getRequestId();
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("Generic error");
        pnRequestError.setGeokey("geokey");
        pnRequestError.setFlowThrow("PREPARE_PHASE_ONE_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("FATY-FATY-2023041520230302");
        pnRequestError.setCreated(Instant.now());

        RuntimeException runtimeException = new RuntimeException("Generic error");

        when(this.requestDeliveryDAO.getByRequestId(any(), anyBoolean()))
                .thenReturn(Mono.error(runtimeException));
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));
        when(requestDeliveryDAO.updateStatus(eq(requestId), eq(statusCode), eq(statusDescription), eq(statusDetail), any())).thenReturn(Mono.empty());

        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("FATY-FATY-2023041520230302")
                .clientId("clientId")
                .iun("FATY-FATY-2023041520230302-101111")
                .attempt(0)
                .build();

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(RuntimeException.class, ex);
                    return true;
                }).verify();

        verify(this.prepareFlowStarter, never()).pushResultPrepareEvent(eq(deliveryRequest), isNull(), eq("clientId"), eq(StatusCodeEnum.KO), isNull());
        verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        assertionItemCapturedWithItemError(itemErrorCaptor, pnRequestError);

        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
    }

    @Test
    void preparePhaseOneAsyncForeignAddressTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_TEST.RECINDEX_0.ATTEMPT_0";
        var iun = "TEST";
        var deliveryRequest = getDeliveryRequest(requestId, iun);
        var address = getAddress();
        address.setCountry("ForeignCountry"); // Indirizzo estero
        var addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);

        when(requestDeliveryDAO.getByRequestId(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, null, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(requestDeliveryDAO.updateDataWithoutGet(deliveryRequest, false)).thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(preparePhaseOneAsyncService.preparePhaseOneAsync(PrepareNormalizeAddressEvent.builder()
                        .requestId(requestId)
                        .iun(iun)
                        .attempt(0)
                        .build()))
                .expectNext(deliveryRequest)
                .verifyComplete();

        verify(sqsSender, times(1)).pushToDelayerToPaperchennelQueue(any());
        verify(prepareFlowStarter, never()).pushPreparePhaseOneOutput(any(), any(), any());
    }



    private void inizialize(){

    }

    private PnDeliveryRequest getDeliveryRequest(String requestId, String iun){
        final PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        deliveryRequest.setAttachments(attachmentInfoList);
        return deliveryRequest;
    }


    private Address getAddress(){
        final Address address = new Address();
        address.setCap("20089");
        address.setCity("Milano");
        address.setCountry("Italia");
        address.setAddress("Via sottosopra");
        address.setPr("MI");
        address.setProductType(RACCOMANDATA_SEMPLICE);
        request.setRequestId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        request.setAddress(address);
        request.setAttempt(0);

        return address;
    }

    private List<PnAttachmentInfo> attachmentInfoList (PnDeliveryRequest deliveryRequest){
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        attachmentInfo.setId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        attachmentInfo.setDate("2023-01-01T00:20:56.630714800Z");
        attachmentInfo.setUrl("");
        attachmentInfo.setDocumentType("pdf");
        attachmentInfo.setFileKey("http://localhost:8080");
        attachmentInfo.setNumberOfPage(0);
        attachmentInfoList.add(attachmentInfo);
        deliveryRequest.setAttachments(attachmentInfoList);
        return attachmentInfoList;
    }

    private List<PnAttachmentInfo> orderedAttachmentInfoList(){
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            PnAttachmentInfo attachment = new PnAttachmentInfo();
            attachment.setId("PAPERTEST.IUN-2023041520230302-101111.RECINDEX_0");
            attachment.setDate("2019-11-07T09:03:08Z");
            attachment.setUrl("http://1234" + (49-i));
            attachment.setDocumentType("pdf");
            attachment.setFileKey(String.valueOf((49-i)));
            attachment.setNumberOfPage(0);
            attachmentInfoList.add(attachment);
        }
        return attachmentInfoList;
    }

}
