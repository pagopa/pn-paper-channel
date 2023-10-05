package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnUntracebleException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.PrepareEventMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.Const.RACCOMANDATA_SEMPLICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PrepareAsyncServiceTest {

    @InjectMocks
    private PrepareAsyncServiceImpl prepareAsyncService;
    @Mock
    private AddressDAO addressDAO;
    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    private SqsSender sqsSender;
    @Mock
    private SafeStorageClient safeStorageClient;
    @Mock
    private AddressManagerClient addressManagerClient;
    @Mock
    private PaperAddressService paperAddressService;
    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;

    private final PrepareAsyncRequest request = new PrepareAsyncRequest();
    private final Address address = new Address();
    private final PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
    private final PnAttachmentInfo attachmentInfo = new PnAttachmentInfo();

    @BeforeEach
    public void setUp(){
        inizialize();
    }

    @Test
    @DisplayName("prepareAsyncTestCorrelationIdNullNotCorrectAddress")
    void prepareAsyncTestCorrelationIdNull(){
        Mockito.when(this.requestDeliveryDAO.getByRequestId(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(getDeliveryRequest()));
        Mockito.when(this.paperAddressService.getCorrectAddress(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new PnAddressFlowException(ExceptionTypeEnum.ADDRESS_NOT_EXIST));

        assertThrows(PnAddressFlowException.class, () -> this.prepareAsyncService.prepareAsync(request).block());

    }

    @Test
    @DisplayName("prepareAsyncTestCorrelationIdCorrectAddressPopulate")
    void prepareAsyncTestCorrelationId(){

        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Mono.just(getDeliveryRequest()));

        Mockito.when(this.paperAddressService.getCorrectAddress(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new Address()));

        Mockito.when(this.addressDAO.create(Mockito.any()))
                .thenReturn(Mono.just(getAddress()));

        Mockito.when(this.addressDAO.findByRequestId(Mockito.any()))
                .thenReturn(Mono.just(getAddress()));

        Mockito.doNothing().when(this.sqsSender).pushPrepareEvent(Mockito.any());

        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(getDeliveryRequest()));

        request.setCorrelationId("FFPAPERTEST.IUN_FATY");
        PnDeliveryRequest deliveryRequest = this.prepareAsyncService.prepareAsync(request).block();
        assertNotNull(deliveryRequest);

    }

    @Test
    @DisplayName("prepareAsyncTestErrorUntraceableAddress")
    void prepareAsyncTestErrorUntraceableAddress(){
        PnDeliveryRequest deliveryRequest = getDeliveryRequest();

        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.paperAddressService.getCorrectAddress(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new PnUntracebleException(new KOReason(FailureDetailCodeEnum.D00, null))));


        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any()))
            .thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(deliveryRequest));

        Mockito.doNothing().when(this.sqsSender).pushPrepareEvent(Mockito.any());

        request.setCorrelationId("FFPAPERTEST.IUN_FATY");

        StepVerifier.create(this.prepareAsyncService.prepareAsync(request))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    return true;
                }).verify();

        // VERIFICO CHE IN QUESTO CASO NON VENGA MAI CREATO IL RECORD DI ERRORE
        Mockito.verify(paperRequestErrorDAO, Mockito.never()).created(Mockito.any(), Mockito.any(), Mockito.any());

        // VERIFICO CHE È STATO INVIATO L'EVENTO DI KOUNREACHABLE A DELIVERY PUSH
        RequestDeliveryMapper.changeState(pnDeliveryRequest, StatusDeliveryEnum.UNTRACEABLE.getCode(),
                StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR.getDescription(), StatusDeliveryEnum.UNTRACEABLE.getDetail(),
                null, null);
        PrepareEvent prepareEventExpected = PrepareEventMapper.toPrepareEvent(deliveryRequest, null, StatusCodeEnum.KO, new KOReason(FailureDetailCodeEnum.D00, null));
        ArgumentCaptor<PrepareEvent> prepareEventArgumentCaptor = ArgumentCaptor.forClass(PrepareEvent.class);

        Mockito.verify(this.sqsSender).pushPrepareEvent(prepareEventArgumentCaptor.capture());
        PrepareEvent prepareEventActual = prepareEventArgumentCaptor.getValue();

        assertThat(prepareEventActual.getRequestId()).isEqualTo(prepareEventExpected.getRequestId());
        assertThat(prepareEventActual.getStatusCode()).isEqualTo(prepareEventExpected.getStatusCode());
        assertThat(prepareEventActual.getStatusDetail()).isEqualTo(prepareEventExpected.getStatusDetail());
        assertThat(prepareEventActual.getFailureDetailCode()).isEqualTo(FailureDetailCodeEnum.D00);
    }

    @Test
    @DisplayName("prepareAsyncTestErrorAttachmentInfoGetUrlNullInvalidSafeStorage")
    void prepareAsyncTestErrorInvalidSafeStorage(){
        PnDeliveryRequest requestDelivery = getDeliveryRequest();

        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(Mono.just(requestDelivery));

        Mockito.when(this.paperAddressService.getCorrectAddress(Mockito.any(), Mockito.any(), Mockito.any()))
                        .thenReturn(Mono.just(new Address()));

        Mockito.when(this.addressDAO.create(Mockito.any()))
                        .thenReturn(Mono.just(getAddress()));

        Mockito.when(this.safeStorageClient.getFile(Mockito.any())).thenReturn(Mono.just(fileDownloadResponseDto()));

        Mockito.doNothing().when(this.sqsSender)
                .pushInternalError(Mockito.any(), Mockito.anyInt(), Mockito.any());

        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any()))
                    .thenReturn(Mono.just(getDeliveryRequest()));

        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(getDeliveryRequest()));

        request.setCorrelationId("FFPAPERTEST.IUN_FATY");
        pnDeliveryRequest.setAttachments(attachmentInfoList());
        StepVerifier.create(this.prepareAsyncService.prepareAsync(request))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("getFileRecursiveErrrorAttemptSaveStorage-1")
    void getFileRecursiveErrror(){
        StepVerifier.create(this.prepareAsyncService.getFileRecursive(-1,"", new BigDecimal(BigInteger.ZERO)))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    return true;
                }).verify();;
    }

    private void inizialize(){
        address.setCap("20089");
        address.setCity("Milano");
        address.setCountry("Italia");
        address.setAddress("Via sottosopra");
        address.setPr("MI");
        address.setProductType(RACCOMANDATA_SEMPLICE);
        request.setRequestId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        request.setAddress(address);
        request.setAttemptRetry(0);

    }

    private PnDeliveryRequest getDeliveryRequest(){
        pnDeliveryRequest.setRequestId("FATY-FATY-2023041520230302");
        pnDeliveryRequest.setIun("FATY-FATY-2023041520230302-101111");
        pnDeliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        pnDeliveryRequest.setAttachments(attachmentInfoList);
        return pnDeliveryRequest;
    }

    private PnAddress getAddress(){
        PnAddress pnAddress = new PnAddress();
        pnAddress.setAddress("Via Milano");
        pnAddress.setCap("20089");
        pnAddress.setPr("MI");
        pnAddress.setCountry("Italia");
        pnAddress.setCity("Milano");
        pnAddress.setFullName("");
        pnAddress.setAddressRow2("");
        pnAddress.setCity2("");
        pnAddress.setNameRow2("");

        return pnAddress;
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
        pnDeliveryRequest.setAttachments(attachmentInfoList);
        return attachmentInfoList;
    }

    private FileDownloadResponseDto fileDownloadResponseDto (){
        FileDownloadResponseDto dto = new FileDownloadResponseDto();
        dto.setDocumentType("pdf");
        dto.setKey("http://localhost:8080");
        dto.setChecksum("ok");
        return dto;
    }

    private DeduplicatesResponseDto getNormalizedAddress(boolean equalityResult){
        DeduplicatesResponseDto dto = new DeduplicatesResponseDto();
        dto.setCorrelationId("122333");
        dto.setEqualityResult(equalityResult);
        AnalogAddressDto address = new AnalogAddressDto();
        address.setAddressRow("Via Milano");
        address.setCap("20089");
        address.setPr("MI");
        address.setCountry("Italia");
        address.setCity("Milano");
        address.setAddressRow2("");
        address.setCity2("");
        dto.setNormalizedAddress(address);
        return dto;
    }

}
