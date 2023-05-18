package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.service.impl.PrepareAsyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.Const.RACCOMANDATA_SEMPLICE;
import static org.junit.jupiter.api.Assertions.*;

class PrepareAsyncServiceTest extends BaseTest {

    @Autowired
    private PrepareAsyncServiceImpl prepareAsyncService;
    @MockBean
    private AddressDAO addressDAO;
    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;
    @MockBean
    private SqsSender sqsSender;
    @MockBean
    private SafeStorageClient safeStorageClient;
    @MockBean
    private AddressManagerClient addressManagerClient;
    @MockBean
    private PaperRequestErrorDAO paperRequestErrorDAO;

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
        Mockito.when(this.requestDeliveryDAO.getByRequestId(Mockito.any())).thenReturn(Mono.just(getDeliveryRequest()));
        Mockito.when(this.addressDAO.findByRequestId(Mockito.any())).thenReturn(Mono.just(getAddress()));
        Mockito.doNothing().when(this.sqsSender).pushPrepareEvent(Mockito.any());
        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(getDeliveryRequest()));
        PnDeliveryRequest deliveryRequest = this.prepareAsyncService.prepareAsync(request).block();
        assertNotNull(deliveryRequest);

    }

    @Test
    @DisplayName("prepareAsyncTestCorrelationIdCorrectAddressPopulate")
    void prepareAsyncTestCorrelationId(){
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any()))
                .thenReturn(Mono.just(getDeliveryRequest()));

        Mockito.when(this.addressManagerClient.deduplicates(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNormalizedAddress(false)));

        Mockito.when(this.addressDAO.findByRequestId(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getAddress()));

        Mockito.when(this.addressDAO.findByRequestId(Mockito.any()))
                .thenReturn(Mono.just(getAddress()));

        Mockito.when(this.addressDAO.create(Mockito.any()))
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
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any()))
                .thenReturn(Mono.just(getDeliveryRequest()));


        Mockito.when(this.addressDAO.findByRequestId(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getAddress()));

        Mockito.when(this.addressManagerClient.deduplicates(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNormalizedAddress(true)));

        Mockito.doNothing().when(this.sqsSender).pushPrepareEvent(Mockito.any());

        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(getDeliveryRequest()));

        Mockito.when(this.paperRequestErrorDAO.created(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnRequestError()));

        request.setCorrelationId("FFPAPERTEST.IUN_FATY");

        StepVerifier.create(this.prepareAsyncService.prepareAsync(request))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("prepareAsyncTestErrorAttachmentInfoGetUrlNullInvalidSafeStorage")
    void prepareAsyncTestErrorInvalidSafeStorage(){
        PnDeliveryRequest requestDelivery = getDeliveryRequest();

        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.any()))
                .thenReturn(Mono.just(requestDelivery));

        Mockito.when(this.addressDAO.findByRequestId(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getAddress()));

        Mockito.when(this.addressManagerClient.deduplicates(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNormalizedAddress(false)));

        Mockito.when(this.addressDAO.create(Mockito.any()))
                .thenReturn(Mono.just(getAddress()));

        Mockito.doNothing().when(this.sqsSender)
                .pushInternalError(Mockito.any(), Mockito.anyInt(), Mockito.any());
        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(getDeliveryRequest()));
        Mockito.when(this.safeStorageClient.getFile(Mockito.any())).thenReturn(Mono.just(fileDownloadResponseDto()));
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
