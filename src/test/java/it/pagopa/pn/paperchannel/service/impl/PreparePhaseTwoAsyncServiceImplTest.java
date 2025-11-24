package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadInfoDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.model.F24Error;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.apache.pdfbox.pdmodel.PDDocument;
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

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.utils.Const.RACCOMANDATA_SEMPLICE;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreparePhaseTwoAsyncServiceImplTest {
    @InjectMocks
    private PreparePhaseTwoAsyncServiceImpl preparePhaseTwoAsyncService;
    @Mock
    private SqsSender sqsSender;
    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;
    @Mock
    private F24Service f24Service;
    @Mock
    private SafeStorageService safeStorageService;
    @Mock
    private AddressDAO addressDAO;
    @Mock
    private PrepareFlowStarter prepareFlowStarter;
    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;
    @Mock
    private CheckCoverageAreaService checkCoverageAreaService;

    private final PnAttachmentInfo attachmentInfo = new PnAttachmentInfo();

    @BeforeEach
    public void setUp(){

    }

    @Test
    void prepareAsyncPhaseTwoRegularFlowOkTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest = getDeliveryRequest(requestId, iun,true);

        var pnAddress = new PnAddress();
        pnAddress.setCity("Cicciano");
        pnAddress.setAddress("Via Leonardo Da Vinci 10");
        pnAddress.setCap("80033");
        pnAddress.setCountry("IT");
        pnAddress.setPr("NA");
        pnAddress.setTypology("TYPOLOGY");

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();


        when(addressDAO.getPnAddress(anyString(), eq(AddressTypeEnum.RECEIVER_ADDRESS), anyBoolean()))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(false);
        // Gli allegati sono già processati (numberOfPage > 0)
        deliveryRequest.getAttachments().forEach(a -> a.setNumberOfPage(1));
        when(addressDAO.findByRequestId(deliveryRequest.getRequestId())).thenReturn(Mono.just(pnAddress));
        when(requestDeliveryDAO.updateDataWithoutGet(deliveryRequest, false)).thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        verify(sqsSender, times(1)).pushPrepareEvent(any());
    }

    @Test
    void prepareAsyncPhaseTwoRegularOkTest() {
        var requestId = "SERVICE_DESK_OPID-12345";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest = getDeliveryRequest(requestId, iun,true);

        var pnAddress = new PnAddress();
        pnAddress.setCity("Cicciano");
        pnAddress.setAddress("Via Leonardo Da Vinci 10");
        pnAddress.setCap("80033");
        pnAddress.setCountry("IT");
        pnAddress.setPr("NA");
        pnAddress.setTypology("TYPOLOGY");

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        when(addressDAO.getPnAddress(anyString(), eq(AddressTypeEnum.RECEIVER_ADDRESS), anyBoolean()))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(false);
        deliveryRequest.getAttachments().forEach(a -> a.setNumberOfPage(1));
        when(addressDAO.findByRequestId(deliveryRequest.getRequestId())).thenReturn(Mono.just(pnAddress));
        when(requestDeliveryDAO.updateDataWithoutGet(deliveryRequest, false)).thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        verify(sqsSender, times(1)).pushPrepareEventOnEventBridge(any(), any());
        verify(sqsSender, never()).pushPrepareEvent(any());
    }


   @Test
    void prepareAsyncPhaseTwoRegularFlowAttachmentsToProcessTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);

        PnAttachmentInfo attachment = new PnAttachmentInfo();
        attachment.setFileKey("fileKey");
        attachment.setNumberOfPage(0);
        deliveryRequest.setAttachments(List.of(attachment));

        var pnAddress = new PnAddress();
        pnAddress.setCity("Cicciano");
        pnAddress.setAddress("Via Leonardo Da Vinci 10");
        pnAddress.setCap("80033");
        pnAddress.setCountry("IT");
        pnAddress.setPr("NA");
        pnAddress.setTypology("TYPOLOGY");

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();
       when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
               .thenReturn(Mono.just(pnAddress));

       when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
               .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(false);

        var fileResponse = new FileDownloadResponseDto();
        var fileInfoDownload = new FileDownloadInfoDto();
        fileInfoDownload.setUrl("http://mocked-url");
        fileResponse.setDownload(fileInfoDownload);
        when(safeStorageService.getFileRecursive(any(), eq(attachment.getFileKey()), any())).thenReturn(Mono.just(fileResponse));
        PDDocument pdDocument = new PDDocument();
        pdDocument.addPage(new org.apache.pdfbox.pdmodel.PDPage());
        pdDocument.addPage(new org.apache.pdfbox.pdmodel.PDPage());
        when(safeStorageService.downloadFile(eq("http://mocked-url"))).thenReturn(Mono.just(pdDocument));

        when(addressDAO.findByRequestId(deliveryRequest.getRequestId())).thenReturn(Mono.just(pnAddress));
        when(requestDeliveryDAO.updateDataWithoutGet(deliveryRequest, false)).thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        verify(sqsSender, times(1)).pushPrepareEvent(any());
    }
    @Test
    void prepareAsyncPhaseTwoUrlNullThrowsGenericException() {
        var requestId = "REQID-URL-NULL";
        var iun = "IUN-URL-NULL";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);

        PnAttachmentInfo attachment = new PnAttachmentInfo();
        attachment.setFileKey("fileKey");
        attachment.setNumberOfPage(0);
        deliveryRequest.setAttachments(List.of(attachment));

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("Url allegato non disponibile");
        pnRequestError.setFlowThrow("PREPARE_PHASE_TWO_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0");
        pnRequestError.setCreated(Instant.now());

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var pnAddress = new PnAddress();
        when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(false);

        var fileInfoDownload = new FileDownloadInfoDto();
        fileInfoDownload.setUrl(null);
        when(safeStorageService.getFileRecursive(any(), eq(attachment.getFileKey()), any())).thenReturn(Mono.error(new PnGenericException(DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage())));
        when(requestDeliveryDAO.updateStatus(eq(requestId), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .verifyComplete();

        verify(sqsSender, times(1)).pushErrorDelayerToPaperChannelAfterSafeStorageErrorQueue(event);
        verify(paperRequestErrorDAO, never()).created(any());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(),descriptionCaptor.capture(),statusDetailCaptor.capture(), any());


        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
    }

    @Test
    void prepareAsyncPhaseTwoAttachmentUrlNullThrowsGenericException() {
        var requestId = "REQID-URL-NULL";
        var iun = "IUN-URL-NULL";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);

        PnAttachmentInfo attachment = new PnAttachmentInfo();
        attachment.setFileKey("fileKey");
        attachment.setUrl(null);
        deliveryRequest.setAttachments(List.of(attachment));

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("Il Safe Storage selezionato è inesistente.");
        pnRequestError.setFlowThrow("PREPARE_PHASE_TWO_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0");
        pnRequestError.setCreated(Instant.now());

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var pnAddress = new PnAddress();

        when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(false);

        when(safeStorageService.getFileRecursive(any(), eq(attachment.getFileKey()), any())).thenReturn(Mono.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage())));
        when(requestDeliveryDAO.updateStatus(eq(requestId), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .verifyComplete();

        verify(sqsSender, times(1)).pushErrorDelayerToPaperChannelAfterSafeStorageErrorQueue(event);
        verify(paperRequestErrorDAO, never()).created(any());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(),descriptionCaptor.capture(),statusDetailCaptor.capture(), any());


        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());

    }


    @Test
    void prepareAsyncPhaseTwoAttachmentDownloadFileThrowsGenericException() {
        var requestId = "REQID-DOWNLOAD-ERR";
        var iun = "IUN-DOWNLOAD-ERR";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);

        PnAttachmentInfo attachment = new PnAttachmentInfo();
        attachment.setFileKey("fileKey");
        attachment.setNumberOfPage(0);
        deliveryRequest.setAttachments(List.of(attachment));

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("Non è stato possibile scaricare il documento");
        pnRequestError.setFlowThrow("PREPARE_PHASE_TWO_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0");
        pnRequestError.setCreated(Instant.now());

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var pnAddress = new PnAddress();

        when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(false);

        var fileResponse = new FileDownloadResponseDto();
        var fileInfoDownload = new FileDownloadInfoDto();
        fileInfoDownload.setUrl("http://mocked-url");
        fileResponse.setDownload(fileInfoDownload);
        when(safeStorageService.getFileRecursive(any(), eq(attachment.getFileKey()), any())).thenReturn(Mono.just(fileResponse));
        when(safeStorageService.downloadFile(eq("http://mocked-url"))).thenReturn(Mono.error(new PnGenericException(DOCUMENT_NOT_DOWNLOADED, DOCUMENT_NOT_DOWNLOADED.getMessage())));
        when(requestDeliveryDAO.updateStatus(eq(requestId), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .verifyComplete();

        verify(sqsSender, times(1)).pushErrorDelayerToPaperChannelAfterSafeStorageErrorQueue(event);
        verify(paperRequestErrorDAO, never()).created(any()); // solo quando finisce i retry scrive nella tabella degli errori
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(),descriptionCaptor.capture(),statusDetailCaptor.capture(), any());


        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
    }

    @Test
    void prepareAsyncPhaseTwoF24FlowOkTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest = getDeliveryRequest(requestId, iun,false);

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var pnAddress = new PnAddress();
        pnAddress.setCity("Cicciano");
        pnAddress.setAddress("Via Leonardo Da Vinci 10");
        pnAddress.setCap("80033");
        pnAddress.setCountry("IT");
        pnAddress.setPr("NA");
        pnAddress.setTypology("TYPOLOGY");
        when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(true);
        when(f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        verify(sqsSender, never()).pushPrepareEvent(any());
    }

    @Test
    void prepareAsyncPhaseTwoErrorInGetRequest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest = getDeliveryRequest(requestId, iun,true);
        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("class java.lang.RuntimeException -> Errore generico");
        pnRequestError.setGeokey(null);
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setFlowThrow("PREPARE_PHASE_TWO_ASYNC_DEFAULT");

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);

        RuntimeException runtimeException = new RuntimeException("Errore generico");

        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true)).thenReturn(Mono.just(new PnAddress()));
        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.error(runtimeException));
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));
        when(requestDeliveryDAO.updateStatus(eq(deliveryRequest.getRequestId()), eq(statusCode), eq(statusDescription), eq(statusDetail), any())).thenReturn(Mono.empty());

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(RuntimeException.class, ex);
                    return true;
                }).verify();

        verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
        verify(requestDeliveryDAO,times(1)).updateStatus(eq(deliveryRequest.getRequestId()), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        PnRequestError pnRequestErrorForAssertion = itemErrorCaptor.getValue();

        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
        Assertions.assertEquals(pnRequestError.getError(),pnRequestErrorForAssertion.getError());
        Assertions.assertEquals(pnRequestError.getFlowThrow(),pnRequestErrorForAssertion.getFlowThrow());
        Assertions.assertEquals(pnRequestError.getCategory(),pnRequestErrorForAssertion.getCategory());
    }

    @Test
    void prepareAsyncPhaseTwoErrorInAttachmentForF24() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setAttachments(new ArrayList<>());

        F24Error f24Error = new F24Error();
        f24Error.setMessage(ExceptionTypeEnum.F24_ERROR.getMessage());
        f24Error.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0");
        f24Error.setAttempt(1);

        PnF24FlowException pnF24FlowException = new PnF24FlowException(ExceptionTypeEnum.F24_ERROR, f24Error, new Throwable());

        ArgumentCaptor<F24Error> f24ErrorArgumentCaptor = ArgumentCaptor.forClass(F24Error.class);

        var pnAddress = new PnAddress();
        pnAddress.setCity("Cicciano");
        pnAddress.setAddress("Via Leonardo Da Vinci 10");
        pnAddress.setCap("80033");
        pnAddress.setCountry("IT");
        pnAddress.setPr("NA");
        pnAddress.setTypology("TYPOLOGY");

        when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(true);
        when(f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.error(pnF24FlowException));
        when(requestDeliveryDAO.updateData(deliveryRequest)).thenReturn(Mono.just(deliveryRequest));

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(PnF24FlowException.class, ex);
                    return true;
                }).verify();

        verify(prepareFlowStarter, times(1)).redrivePreparePhaseTwoAfterF24Error(f24ErrorArgumentCaptor.capture());
        verify(requestDeliveryDAO, times(1)).updateData(deliveryRequest);

        F24Error f24ErrorForAssertion = f24ErrorArgumentCaptor.getValue();
        Assertions.assertEquals(f24Error,f24ErrorArgumentCaptor.getValue());
        Assertions.assertEquals(f24Error.getMessage(),f24ErrorForAssertion.getMessage());
    }
    @Test
    void prepareAsyncPhaseTwoErrorInAttachmentForF24_PnInternalException() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setAttachments(new ArrayList<>());

        PnInternalException pnInternalException = new PnInternalException("missing URL f24set on f24serviceImpl", PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR);

        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setError("class it.pagopa.pn.commons.exceptions.PnInternalException -> Internal Server Error");
        pnRequestError.setFlowThrow("PREPARE_PHASE_TWO_ASYNC_DEFAULT");
        pnRequestError.setCause("UNKNOWN##"+ Instant.now().toString());
        pnRequestError.setCategory("UNKNOWN");
        pnRequestError.setAuthor("PN-PAPER-CHANNEL");
        pnRequestError.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0");
        pnRequestError.setCreated(Instant.now());

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);
        ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);

        var pnAddress = new PnAddress();
        pnAddress.setCity("Cicciano");
        pnAddress.setAddress("Via Leonardo Da Vinci 10");
        pnAddress.setCap("80033");
        pnAddress.setCountry("IT");
        pnAddress.setPr("NA");
        pnAddress.setTypology("TYPOLOGY");

        when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
                .thenReturn(Mono.just(pnAddress));

        when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                .thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));
        when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(true);
        when(f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.error(pnInternalException));
        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));
        when(requestDeliveryDAO.updateStatus(eq(requestId), eq(statusCode), eq(statusDescription), eq(statusDetail), any())).thenReturn(Mono.empty());

        PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(PnInternalException.class, ex);
                    return true;
                }).verify();

        verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
        verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

        PnRequestError pnRequestErrorForAssertion = itemErrorCaptor.getValue();

        Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
        Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
        Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
        Assertions.assertEquals(pnRequestError.getError(),pnRequestErrorForAssertion.getError());
        Assertions.assertEquals(pnRequestError.getFlowThrow(),pnRequestErrorForAssertion.getFlowThrow());
        Assertions.assertEquals(pnRequestError.getCategory(),pnRequestErrorForAssertion.getCategory());
    }

    @Test
        void prepareAsyncPhaseTwoErrorInAttachmentForF24_RuntimeException() {
            var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
            var iun = "GJWA-HMEK-RGUJ-202307-H-1";
            PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
            deliveryRequest.setRequestId(requestId);
            deliveryRequest.setIun(iun);
            deliveryRequest.setAttachments(new ArrayList<>());

            RuntimeException runtimeException = new RuntimeException("Errore generico");

            PnRequestError pnRequestError = new PnRequestError();
            pnRequestError.setError("class java.lang.RuntimeException -> Errore generico");
            pnRequestError.setFlowThrow("PREPARE_PHASE_TWO_ASYNC_DEFAULT");
            pnRequestError.setCause("UNKNOWN##" + Instant.now().toString());
            pnRequestError.setCategory("UNKNOWN");
            pnRequestError.setAuthor("PN-PAPER-CHANNEL");
            pnRequestError.setRequestId(requestId);
            pnRequestError.setCreated(Instant.now());

            ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> statusCodeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> statusDetailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PnRequestError> itemErrorCaptor = ArgumentCaptor.forClass(PnRequestError.class);

            StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
            String statusCode = statusDeliveryEnum.getCode();
            String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
            String statusDetail = statusDeliveryEnum.getDetail();

            var pnAddress = new PnAddress();

            when(requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false)).thenReturn(Mono.just(deliveryRequest));

            when(addressDAO.getPnAddress(requestId, AddressTypeEnum.RECEIVER_ADDRESS, true))
                    .thenReturn(Mono.just(pnAddress));

            when(checkCoverageAreaService.filterAttachmentsToSend(any(), any(), any()))
                    .thenReturn(Mono.just(deliveryRequest));
            when(f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)).thenReturn(true);
            when(f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.error(runtimeException));
            when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(pnRequestError));
            when(requestDeliveryDAO.updateStatus(eq(requestId), eq(statusCode), eq(statusDescription), eq(statusDetail), any())).thenReturn(Mono.empty());

            PnPrepareDelayerToPaperchannelPayload event = PnPrepareDelayerToPaperchannelPayload.builder()
                    .requestId(requestId)
                    .iun(iun)
                    .attempt(0)
                    .build();

            StepVerifier.create(preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(event))
                    .expectErrorMatches(ex -> ex instanceof RuntimeException)
                    .verify();

            verify(paperRequestErrorDAO, times(1)).created(itemErrorCaptor.capture());
            verify(requestDeliveryDAO, times(1)).updateStatus(eq(requestId), statusCodeCaptor.capture(), descriptionCaptor.capture(), statusDetailCaptor.capture(), any());

            PnRequestError pnRequestErrorForAssertion = itemErrorCaptor.getValue();

            Assertions.assertEquals(statusDescription, descriptionCaptor.getValue());
            Assertions.assertEquals(statusCode, statusCodeCaptor.getValue());
            Assertions.assertEquals(statusDetail, statusDetailCaptor.getValue());
            Assertions.assertEquals(pnRequestError.getError(), pnRequestErrorForAssertion.getError());
            Assertions.assertEquals(pnRequestError.getFlowThrow(), pnRequestErrorForAssertion.getFlowThrow());
            Assertions.assertEquals(pnRequestError.getCategory(), pnRequestErrorForAssertion.getCategory());
        }

    private void inizialize(){}

    private PnDeliveryRequest getDeliveryRequest(String requestId, String iun,boolean searchAttacchmentInfo){
        final PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);
        List<PnAttachmentInfo> attachmentInfoList;
        if(searchAttacchmentInfo){
            attachmentInfoList = attachmentInfoList();
        } else {
            attachmentInfoList = attachmentInfoListF24();
        }
        deliveryRequest.setAttachments(attachmentInfoList);
        return deliveryRequest;
    }

    private List<PnAttachmentInfo> attachmentInfoList (){
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        attachmentInfo.setId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        attachmentInfo.setDate("2023-01-01T00:20:56.630714800Z");
        attachmentInfo.setUrl("");
        attachmentInfo.setDocumentType("pdf");
        attachmentInfo.setFileKey("http://localhost:8080");
        attachmentInfo.setNumberOfPage(0);
        attachmentInfoList.add(attachmentInfo);
        return attachmentInfoList;
    }

    private List<PnAttachmentInfo> attachmentInfoListF24 (){
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        attachmentInfo.setId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        attachmentInfo.setDate("2023-01-01T00:20:56.630714800Z");
        attachmentInfo.setUrl("");
        attachmentInfo.setDocumentType("PN_F24_SET");
        attachmentInfo.setFileKey("f24set");
        attachmentInfo.setNumberOfPage(0);
        attachmentInfoList.add(attachmentInfo);
        return attachmentInfoList;
    }


}
