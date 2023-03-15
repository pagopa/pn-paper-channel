package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;
class PaperChannelServiceTest extends BaseTest {

    @Autowired
    private PaperChannelServiceImpl paperChannelService;
    @MockBean
    private CostDAO costDAO;
    @MockBean
    private DeliveryDriverDAO deliveryDriverDAO;
    @MockBean
    private TenderDAO tenderDAO;
    @MockBean
    private ExcelDAO<DeliveriesData> excelDAO;
    @MockBean
    private FileDownloadDAO fileDownloadDAO;
    @MockBean
    private S3Bucket s3Bucket;


    @Test
    @DisplayName("whenRetrieveAllTendersFromPageOne")
    void getAllTenderWithElementInPageTest(){
        Mockito.when(this.tenderDAO.getTenders())
                .thenReturn(Mono.just(getListTender(5)));

        PageableTenderResponseDto response = this.paperChannelService.getAllTender(1, 10).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(true, response.getLast());
        assertEquals(6, response.getNumberOfElements());
        assertEquals(0, response.getNumber());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllTendersFromPageOneWithMorePage")
    void getAllTenderWithElementInPageWithMorePageTest(){
        Mockito.when(this.tenderDAO.getTenders())
                .thenReturn(Mono.just(getListTender(25)));

        PageableTenderResponseDto response = this.paperChannelService.getAllTender(1, 10).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(false, response.getLast());
        assertEquals(10, response.getNumberOfElements());
        assertEquals(26, response.getTotalElements());
        assertEquals(0, response.getNumber());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveDetailTenderThenReturnResponse")
    void getDetailTenderFromCode(){
        Mockito.when(tenderDAO.getTender("1234"))
                .thenReturn(Mono.just(getListTender(1).get(0)));

        TenderDetailResponseDTO response = this.paperChannelService.getTenderDetails("1234").block();
        assertNotNull(response);
        assertEquals(true, response.getResult());
        assertEquals(TenderDetailResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertEquals("Tender_0", response.getTender().getCode());
    }

    @Test
    @DisplayName("whenRetrieveDetailTenderNotExistThenThrowError")
    void getDetailTenderFromCodeThrowError(){
        Mockito.when(tenderDAO.getTender("1234"))
                .thenReturn(Mono.empty());
        StepVerifier.create(this.paperChannelService.getTenderDetails("1234"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(TENDER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());return true;
                }).verify();
    }

    @Test
    @DisplayName("whenRetrieveDetailDriverThenReturnResponse")
    void getDetailDriverFromCode(){
        Mockito.when(deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getListDrivers(1, false).get(0)));

        DeliveryDriverResponseDTO response = this.paperChannelService.getDriverDetails("1234", "1234").block();
        assertNotNull(response);
        assertEquals(true, response.getResult());
        assertEquals(DeliveryDriverResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertEquals("123456780", response.getDriver().getTaxId());
    }

    @Test
    @DisplayName("whenRetrieveDetailDriverNotExistThenThrowError")
    void getDetailDriverFromCodeThatNotExistThrowError(){
        Mockito.when(deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.getDriverDetails("1234", "1234"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());return true;
                }).verify();
    }

    @Test
    @DisplayName("whenRetrieveDetailFSUThenReturnResponse")
    void getDetailFSUFromCode(){
        Mockito.when(deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(getListDrivers(1, true).get(0)));

        FSUResponseDTO response = this.paperChannelService.getDetailsFSU("1234").block();
        assertNotNull(response);
        assertEquals(true, response.getResult());
        assertEquals(FSUResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertEquals("123456780", response.getFsu().getTaxId());
    }

    @Test
    @DisplayName("whenRetrieveDetailDriverNotExistThenThrowError")
    void getDetailFSUFromCodeThatNotExistThrowError(){
        Mockito.when(deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.getDetailsFSU("1234"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());return true;
                }).verify();
    }

    @Test
    @DisplayName("whenRetrieveAllDriversFromPageOne")
    void getAllDriversWithElementInPageTest(){
        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getListDrivers(5, false).stream()));

        PageableDeliveryDriverResponseDto response =
                this.paperChannelService.getAllDeliveriesDrivers("1234", 1, 10, true).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(true, response.getLast());
        assertEquals(5, response.getNumberOfElements());
        assertEquals(0, response.getNumber());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllDriversFromPageOneWithMorePage")
    void getAllDriversWithElementInPageWithMorePageTest(){
        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getListDrivers(25, false).stream()));

        PageableDeliveryDriverResponseDto response =
                this.paperChannelService.getAllDeliveriesDrivers("1234", 1, 10, true).block();

        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(false, response.getLast());
        assertEquals(10, response.getNumberOfElements());
        assertEquals(25, response.getTotalElements());
        assertEquals(0, response.getNumber());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllCostsFromPageOne")
    void getAllCostsWithElementInPageTest(){
        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("1234","1234", false).stream()));

        PageableCostResponseDto response =
                this.paperChannelService.getAllCostFromTenderAndDriver("1234","1234", 1, 10).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(true, response.getLast());
        assertEquals(9, response.getNumberOfElements());
        assertEquals(0, response.getNumber());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllCostsFromPageOneWithMorePage")
    void getAllCostsWithElementInPageWithMorePageTest(){
        List<PnCost> moreCost = new ArrayList<>();
        moreCost.addAll(getAllCosts("1234", "12345", false));
        moreCost.addAll(getAllCosts("1234", "12346", true));
        moreCost.addAll(getAllCosts("1234", "12347", true));
        moreCost.addAll(getAllCosts("1234", "12348", false));
        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(moreCost.stream()));

        PageableCostResponseDto response =
                this.paperChannelService.getAllCostFromTenderAndDriver("1234","1234", 1, 10).block();

        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(false, response.getLast());
        assertEquals(10, response.getNumberOfElements());
        assertEquals(36, response.getTotalElements());
        assertEquals(0, response.getNumber());
        assertEquals(4, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrievePresignedUrlThenResponse")
    void getPresignedUrlOK(){
        PresignedUrlResponseDto mocked = getPresignedDTO();
        Mockito.when(s3Bucket.presignedUrl()).thenReturn(Mono.just(mocked));
        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(new PnDeliveryFile()));
        PresignedUrlResponseDto response = this.paperChannelService.getPresignedUrl().block();
        assertNotNull(response);
        assertEquals(mocked.getUuid(), response.getUuid());
        assertEquals(mocked.getPresignedUrl(), response.getPresignedUrl());
    }

    @Test
    @DisplayName("whenDownloadExcelFirstRequestAndFileNotReady")
    void downloadExcelTenderFirstRequestAndFileNotReady(){
        PnDeliveryFile file = new PnDeliveryFile();
        file.setStatus(InfoDownloadDTO.StatusEnum.UPLOADING.getValue());
        file.setUuid("UUID_FILE");
        Mockito.when(fileDownloadDAO.create(Mockito.any())).thenReturn(Mono.just(file));
        InfoDownloadDTO response = this.paperChannelService.downloadTenderFile("1234", null).block();
        assertNotNull(response);
        assertEquals(file.getStatus(), response.getStatus().getValue());
        assertEquals(file.getUuid(), response.getUuid());
        assertNull(response.getData());
        assertNotNull(response.getRetryAfter());
    }

    @Test
    @DisplayName("whenDownloadExcelSecondRequestAndFileNotReady")
    void downloadExcelTenderSecondRequestAndFileNotReady(){
        PnDeliveryFile file = new PnDeliveryFile();
        file.setStatus(InfoDownloadDTO.StatusEnum.UPLOADING.getValue());
        file.setUuid("UUID_FILE");
        Mockito.when(fileDownloadDAO.getUuid(Mockito.any())).thenReturn(Mono.just(file));
        Mockito.when(s3Bucket.getObjectData(Mockito.any())).thenReturn(null);
        InfoDownloadDTO response = this.paperChannelService.downloadTenderFile("1234", "UUID_FILE").block();
        assertNotNull(response);
        assertEquals(file.getStatus(), response.getStatus().getValue());
        assertEquals(file.getUuid(), response.getUuid());
        assertNull(response.getData());
        assertNotNull(response.getRetryAfter());
    }

    @Test
    @DisplayName("whenDownloadExcelThirdRequestAndFileReady")
    void downloadExcelTenderThirdRequestAndFileReady(){
        PnDeliveryFile file = new PnDeliveryFile();
        file.setStatus(InfoDownloadDTO.StatusEnum.UPLOADED.getValue());
        file.setUuid("UUID_FILE");
        Mockito.when(fileDownloadDAO.getUuid(Mockito.any())).thenReturn(Mono.just(file));
        Mockito.when(s3Bucket.getObjectData(Mockito.any())).thenReturn("Byte mock".getBytes());
        InfoDownloadDTO response = this.paperChannelService.downloadTenderFile("1234", "UUID_FILE").block();
        assertNotNull(response);
        assertEquals(file.getStatus(), response.getStatus().getValue());
        assertEquals(file.getUuid(), response.getUuid());
        assertNotNull(response.getData());
        assertNull(response.getRetryAfter());
    }

    @Test
    @DisplayName("whenDownloadExcelSecondRequestAndFileEntityNotExistedThrowException")
    void downloadExcelTenderSecondRequestButFileEntityNotExisted(){

        Mockito.when(fileDownloadDAO.getUuid(Mockito.any())).thenReturn(Mono.empty());
        StepVerifier.create(this.paperChannelService.downloadTenderFile("1234", "UUUIII"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(DELIVERY_REQUEST_NOT_EXIST, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenTryDeleteTenderWithTenderStatusDifferentToCreatedThenThrowException")
    void deleteTenderWithTenderStatusInProgress(){
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(tender));

        StepVerifier.create(this.paperChannelService.deleteTender("123"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(TENDER_CANNOT_BE_DELETED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }
    @Test
    @DisplayName("whenTryDeleteDriverWithTenderCorrectStatusThenReturnOK")
    void deleteTenderOK(){
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(tender));

        Mockito.when(this.tenderDAO.deleteTender(Mockito.any()))
                .thenReturn(Mono.just(tender));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("1234", "1223", true).stream()));

        Mockito.when(this.costDAO.deleteCost(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnCost()));

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getListDrivers(1, true).stream()));

        Mockito.when(this.deliveryDriverDAO.deleteDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnDeliveryDriver()));

        StepVerifier.create(this.paperChannelService.deleteTender("123"))
                .verifyComplete();
    }

    @Test
    @DisplayName("whenTryDeleteDriverWithTenderStatusDifferentToCreatedThenThrowException")
    void deleteDriverWithTenderStatusInProgress(){
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(tender));
        StepVerifier.create(this.paperChannelService.deleteDriver("123", "12233"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(DRIVER_CANNOT_BE_DELETED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }
    @Test
    @DisplayName("whenTryDeleteDriverWithTenderCorrectStatusThenReturnOK")
    void deleteDriverOK(){
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(tender));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("1234", "1223", true).stream()));

        Mockito.when(this.costDAO.deleteCost(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnCost()));

        Mockito.when(this.deliveryDriverDAO.deleteDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnDeliveryDriver()));

        StepVerifier.create(this.paperChannelService.deleteDriver("123", "1223"))
                .verifyComplete();
    }

    @Test
    @DisplayName("whenTryDeleteCostWithTenderStatusDifferentToCreatedThenThrowException")
    void deleteCostWithTenderStatusInProgress(){
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(tender));
        StepVerifier.create(this.paperChannelService.deleteCost("123", "12233", "UUID"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(COST_CANNOT_BE_DELETED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenTryDeleteCostWithTenderCorrectStatusThenReturnOK")
    void deleteCostOK(){
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(tender));
        Mockito.when(this.costDAO.deleteCost(Mockito.any(), Mockito.any()))
                        .thenReturn(Mono.just(new PnCost()));

        StepVerifier.create(this.paperChannelService.deleteCost("123", "1223", "UUID"))
                .verifyComplete();
    }


    @Test
    @DisplayName("whenChangeStatusTenderThatNotExistedThrowException")
    void updateStatusTenderThatNotExisted(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.CREATED);
        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.empty());
        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(TENDER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderInProgressOrEndedThrowException")
    void updateStatusTenderInProgressOrEnded(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.CREATED);
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.ENDED.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));
        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(STATUS_NOT_VARIABLE, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();

        tender.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));
        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(STATUS_NOT_VARIABLE, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderAndExistedOtherTenderAlreadyConsolidatedForIntervalTimeThrowException")
    void updateStatusTenderAndExistedOtherTenderAlreadyConsolidated(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(tender));

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(CONSOLIDATE_ERROR, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithoutFSUThrowException")
    void updateStatusTenderWithoutFSU(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(COST_DRIVER_OR_FSU_NOT_FOUND, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithFSUAndWithoutCostThrowException")
    void updateStatusTenderWithFSUAndWithoutCost(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        PnDeliveryDriver fsu = this.getListDrivers(1, true).get(0);

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(fsu));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(new ArrayList<PnCost>().stream()));

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(COST_DRIVER_OR_FSU_NOT_FOUND, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderInvalidCostThrowException")
    void updateStatusTenderWithInvalidCost(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        PnDeliveryDriver fsu = this.getListDrivers(1, true).get(0);

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(fsu));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("123", fsu.getTaxId(), false).stream()));

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(FSUCOST_VALIDATOR_NOTVALID, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithValidCostThenResponseOK")
    void updateStatusTenderWithValidCost(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        PnDeliveryDriver fsu = this.getListDrivers(1, true).get(0);

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(fsu));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("123", fsu.getTaxId(), true).stream()));

        PnTender tenderUpdated = this.getListTender(1).get(0);
        tenderUpdated.setStatus(TenderDTO.StatusEnum.VALIDATED.toString());
        Mockito.when(this.tenderDAO.createOrUpdate(Mockito.any())).thenReturn(Mono.just(tenderUpdated));

        TenderCreateResponseDTO response = this.paperChannelService.updateStatusTender("123", status).block();
        assertNull(response);
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithCreatedStatus")
    void updateStatusTenderWithCreatedStatus(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.CREATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.VALIDATED.toString());
        tender.setStartDate(Instant.now().plus(10, ChronoUnit.DAYS));
        tender.setEndDate(Instant.now().plus(15, ChronoUnit.DAYS));
        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        PnTender tenderUpdated = this.getListTender(1).get(0);
        tenderUpdated.setStatus(TenderDTO.StatusEnum.CREATED.toString());
        Mockito.when(this.tenderDAO.createOrUpdate(Mockito.any())).thenReturn(Mono.just(tenderUpdated));

        TenderCreateResponseDTO response = this.paperChannelService.updateStatusTender("123", status).block();
        assertNull(response);
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithSameStatus")
    void updateStatusTenderWithSameStatus(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.VALIDATED.toString());
        tender.setStartDate(Instant.now().plus(10, ChronoUnit.DAYS));
        tender.setEndDate(Instant.now().plus(15, ChronoUnit.DAYS));

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));


        TenderCreateResponseDTO response = this.paperChannelService.updateStatusTender("123", status).block();
        assertNull(response);
    }

    private List<PnCost> getAllCosts(String tenderCode, String driverCode, boolean fsu){
        List<PnCost> costs = new ArrayList<>();
        List<ProductTypeEnum> products = List.of(ProductTypeEnum.AR, ProductTypeEnum._890, ProductTypeEnum.RS);
        List<String> zones = List.of("ZONE_1", "ZONE_2", "ZONE_3");
        List<String> caps = new ArrayList<>(List.of("21222", "11111"));
        if (fsu) caps.add(Const.CAP_DEFAULT);
        for (ProductTypeEnum national: products){
            PnCost cost = getCost(null, caps, national.getValue());
            cost.setTenderCode(tenderCode);
            cost.setDeliveryDriverCode(driverCode);
            cost.setUuid(UUID.randomUUID().toString());
            cost.setFsu(fsu);
            costs.add(cost);
        }
        for (String zone : zones){
            PnCost cost = getCost(zone, null, "AR");
            cost.setTenderCode(tenderCode);
            cost.setDeliveryDriverCode(driverCode);
            cost.setUuid(UUID.randomUUID().toString());
            cost.setFsu(fsu);
            costs.add(cost);
        }
        for (String zone : zones){
            PnCost cost = getCost(zone, null, "RS");
            cost.setTenderCode(tenderCode);
            cost.setDeliveryDriverCode(driverCode);
            cost.setUuid(UUID.randomUUID().toString());
            cost.setFsu(fsu);
            costs.add(cost);
        }

        return costs;
    }

    private PnCost getCost(String zone, List<String> cap, String productType){
        PnCost cost = new PnCost();
        cost.setTenderCode("TENDER_1");
        cost.setFsu(true);
        cost.setZone(zone);
        cost.setCap(cap);
        cost.setUuid(UUID.randomUUID().toString());
        cost.setBasePrice(1.23F);
        cost.setPagePrice(1.23F);
        cost.setProductType(productType);
        return cost;
    }

    private List<PnDeliveryDriver> getListDrivers(int number, boolean fsu){
        List<PnDeliveryDriver> drivers = new ArrayList<>();
        for (int i = 0; i < number; i++){
            PnDeliveryDriver driver = new PnDeliveryDriver();
            driver.setTenderCode("TENDER_1");
            driver.setFsu(fsu);
            driver.setTaxId("12345678"+i);
            drivers.add(driver);
        }
        return drivers;
    }

    private List<PnTender> getListTender(int number) {
        List<PnTender> tenders = new ArrayList<>();
        for (int i=0; i <= number; i++){
            PnTender tender = new PnTender();
            tender.setTenderCode("Tender_"+i);
            tender.setStatus("CREATED");
            tenders.add(tender);
        }
        return tenders;
    }

    private PresignedUrlResponseDto getPresignedDTO(){
        PresignedUrlResponseDto dto = new PresignedUrlResponseDto();
        dto.setUuid("UUID_PRESIGNED");
        dto.setPresignedUrl("URL_PRESIGNED");
        return dto;
    }

}