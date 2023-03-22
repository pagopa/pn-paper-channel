package it.pagopa.pn.paperchannel.service;

import io.swagger.models.auth.In;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.DeliveryDriverMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
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

    private MockedStatic<DeliveryDriverMapper> mockedStaticDelivery;

    @AfterEach
    void after(){
        if (mockedStaticDelivery != null){
            mockedStaticDelivery.close();
        }
    }

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
    @DisplayName("whenCalledNotifyUploadWithoutUuidThenThrowException")
    void notifyUploadWithoutUUID(){
        StepVerifier.create(this.paperChannelService.notifyUpload("1234", new NotifyUploadRequestDto()))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(BADLY_REQUEST, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCalledNotifyUploadButFileRequestNotExistedThenThrowException")
    void notifyUploadButFileNotExisted(){
        NotifyUploadRequestDto request = new NotifyUploadRequestDto();
        request.setUuid("UUID_REQUEST");

        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.notifyUpload("1234", request))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(FILE_REQUEST_ASYNC_NOT_FOUND, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusInErrorThrowException")
    void notifyUploadWithFileStatusInError(){
        NotifyUploadRequestDto request = new NotifyUploadRequestDto();
        request.setUuid("UUID_REQUEST");
        PnErrorMessage errorMessage = new PnErrorMessage();
        errorMessage.setMessage("Error With file");
        PnDeliveryFile file = new PnDeliveryFile();
        file.setStatus(FileStatusCodeEnum.ERROR.getCode());
        file.setErrorMessage(errorMessage);

        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                        .thenReturn(Mono.just(file));

        StepVerifier.create(this.paperChannelService.notifyUpload("1234", request))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(EXCEL_BADLY_CONTENT, ((PnGenericException) e).getExceptionType());
                    assertEquals(errorMessage.getMessage(), e.getMessage());
                    return true;
                }).verify();

        PnErrorDetails errorDetails = new PnErrorDetails();
        errorDetails.setCol(2);
        errorDetails.setRow(2);
        errorDetails.setMessage("Errore riga");
        errorDetails.setColName("TAX ID");
        errorMessage.setErrorDetails(List.of(errorDetails));

        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(file));

        StepVerifier.create(this.paperChannelService.notifyUpload("1234", request))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnExcelValidatorException);
                    assertEquals(BADLY_REQUEST, ((PnExcelValidatorException) e).getErrorType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusIsUploadedThenResponse")
    void notifyUploadWithFileStatusInUploaded(){
        NotifyUploadRequestDto request = new NotifyUploadRequestDto();
        request.setUuid("UUID_REQUEST");
        PnDeliveryFile file = new PnDeliveryFile();
        file.setStatus(FileStatusCodeEnum.UPLOADED.getCode());
        file.setFilename("FILENAME");
        file.setUrl("URL");
        file.setUuid("UUID_REQUEST");


        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(file));

        NotifyResponseDto dto = this.paperChannelService.notifyUpload("1234", request).block();
        assertNotNull(dto);
        assertNull(dto.getRetryAfter());
        assertEquals(NotifyResponseDto.StatusEnum.COMPLETE, dto.getStatus());
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusIsUploadingAndS3NotHaveAFileThenResponseRetryAfter")
    void notifyUploadWithFileStatusInUploadingAndS3NoFIle(){
        NotifyUploadRequestDto request = new NotifyUploadRequestDto();
        request.setUuid("UUID_REQUEST");
        PnDeliveryFile file = new PnDeliveryFile();
        file.setStatus(FileStatusCodeEnum.UPLOADING.getCode());
        file.setFilename("FILENAME");
        file.setUrl("URL");
        file.setUuid("UUID_REQUEST");

        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(file));

        Mockito.when(s3Bucket.getFileInputStream(Mockito.any()))
                .thenReturn(null);

        NotifyResponseDto dto = this.paperChannelService.notifyUpload("1234", request).block();
        assertNotNull(dto);
        assertNotNull(dto.getRetryAfter());
        assertEquals(NotifyResponseDto.StatusEnum.IN_PROGRESS, dto.getStatus());
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusIsUploadingAndS3HaveAFileThenStartNotifyAsync")
    void notifyUploadWithFileStatusInUploadingAndS3haveFile(){
        PaperChannelServiceImpl spyPaperChannelService = Mockito.spy(this.paperChannelService);

        NotifyUploadRequestDto request = new NotifyUploadRequestDto();
        request.setUuid("UUID_REQUEST");
        PnDeliveryFile file = new PnDeliveryFile();
        file.setStatus(FileStatusCodeEnum.UPLOADING.getCode());
        file.setFilename("FILENAME");
        file.setUrl("URL");
        file.setUuid("UUID_REQUEST");

        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(file));

        Mockito.when(s3Bucket.getFileInputStream(Mockito.any()))
                .thenReturn(getInputStream());

        Mockito.when(spyPaperChannelService.notifyUploadAsync(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        PnDeliveryFile file1 = new PnDeliveryFile();
        file1.setStatus(FileStatusCodeEnum.IN_PROGRESS.getCode());
        file1.setFilename("FILENAME");
        file1.setUrl("URL");
        file1.setUuid("UUID_REQUEST");
        Mockito.when(this.fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(file1));

        NotifyResponseDto dto = spyPaperChannelService.notifyUpload("1234", request).block();
        assertNotNull(dto);

        assertNotNull(dto.getRetryAfter());
        assertEquals(NotifyResponseDto.StatusEnum.IN_PROGRESS, dto.getStatus());
    }


    @Test
    @DisplayName("whenCallNotifyAsyncWithExcelBadlyContentThenSaveFileRequestWithError")
    void notifyUploadWithErrorExcelValidation(){
        PnDeliveryFile file1 = new PnDeliveryFile();
        file1.setStatus(FileStatusCodeEnum.UPLOADED.getCode());
        file1.setFilename("FILENAME");
        file1.setUrl("URL");
        file1.setUuid("UUID_REQUEST");

        Mockito.when(this.excelDAO.readData(Mockito.any()))
                .thenThrow(new PnGenericException(EXCEL_BADLY_CONTENT, "ERROR VALIDATION"));

        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(file1));

        StepVerifier.create(this.paperChannelService.notifyUploadAsync(file1, getInputStream(), "1122"))
                .expectError(PnGenericException.class)
                .verify();
    }

    @Test
    @DisplayName("whenCallNotifyAsyncWithBadlyMappingThenThrowException")
    void notifyUploadWithErrorMappingFromExcel(){
        PnDeliveryFile file1 = new PnDeliveryFile();
        file1.setStatus(FileStatusCodeEnum.UPLOADED.getCode());
        file1.setFilename("FILENAME");
        file1.setUrl("URL");
        file1.setUuid("UUID_REQUEST");

        Mockito.when(this.excelDAO.readData(Mockito.any()))
                .thenReturn(new DeliveriesData());

        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(file1));

        mockedStaticDelivery = Mockito.mockStatic(DeliveryDriverMapper.class);
        mockedStaticDelivery.when(() -> {
            DeliveryDriverMapper.toEntityFromExcel(Mockito.any(), Mockito.any());
        }).thenThrow(new PnExcelValidatorException(ExceptionTypeEnum.INVALID_CAP_PRODUCT_TYPE, null));

        StepVerifier.create(this.paperChannelService.notifyUploadAsync(file1, getInputStream(), "1122"))
                .expectError(PnExcelValidatorException.class)
                .verify();
    }

    //@Test
    //@DisplayName("whenCallNotifyAsyncWithExceptionCreateDriverThenThrowException")
    void notifyUploadAsyncErrorWithCreateDriver(){
        PnDeliveryFile file1 = new PnDeliveryFile();
        file1.setStatus(FileStatusCodeEnum.UPLOADED.getCode());
        file1.setFilename("FILENAME");
        file1.setUrl("URL");
        file1.setUuid("UUID_REQUEST");
        Map<PnDeliveryDriver, List<PnCost>> map = new HashMap<>();
        map.put(getListDrivers(1, true).get(0), getAllCosts("1234", "1234", true));

        Mockito.when(this.excelDAO.readData(Mockito.any()))
                .thenReturn(new DeliveriesData());

        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(file1));

        mockedStaticDelivery = Mockito.mockStatic(DeliveryDriverMapper.class);
        mockedStaticDelivery.when(() -> {
            DeliveryDriverMapper.toEntityFromExcel(Mockito.any(), Mockito.any());
        }).thenReturn(map);

        Mockito.when(deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getListDrivers(5, true).stream()));

        Mockito.when(costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("533", "444", true).stream()));


        Mockito.when(tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(getListTender(1).get(0)));

        Mockito.when(paperChannelService.deleteDriver(Mockito.any(), Mockito.any()))
                    .thenReturn(Mono.empty());


        Mockito.when(this.deliveryDriverDAO.createOrUpdate(Mockito.any()))
                    .thenThrow(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage()));


        StepVerifier.create(this.paperChannelService.notifyUploadAsync(file1, getInputStream(), "1122"))
                .expectError(PnGenericException.class)
                .verify();
    }

    @Test
    @DisplayName("whenCallNotifyAsyncWithCorrectDataThenUpdateStatus")
    void notifyUploadAsyncWithCorrectDataThenUpdateStatus(){
        PaperChannelServiceImpl spyPaperChannel = Mockito.spy(this.paperChannelService);
        PnDeliveryFile file1 = new PnDeliveryFile();
        file1.setStatus(FileStatusCodeEnum.UPLOADED.getCode());
        file1.setFilename("FILENAME");
        file1.setUrl("URL");
        file1.setUuid("UUID_REQUEST");
        Map<PnDeliveryDriver, List<PnCost>> map = new HashMap<>();
        map.put(getListDrivers(1, true).get(0), getAllCosts("1234", "1234", true));

        Mockito.when(excelDAO.readData(Mockito.any()))
                .thenReturn(new DeliveriesData());

        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(file1));

        mockedStaticDelivery = Mockito.mockStatic(DeliveryDriverMapper.class);
        mockedStaticDelivery.when(() -> {
            DeliveryDriverMapper.toEntityFromExcel(Mockito.any(), Mockito.any());
        }).thenReturn(map);

        Mockito.when(deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getListDrivers(5, true).stream()));



        Mockito.doReturn(Mono.empty()).when(spyPaperChannel).deleteDriver(Mockito.any(), Mockito.any());


        Mockito.when(deliveryDriverDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(getListDrivers(1, true).get(0)));

        Mockito.when(costDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(getCost("ZONE_1", null,"AR")));

        Mockito.when(fileDownloadDAO.create(Mockito.any())).thenReturn(Mono.just(file1));

        StepVerifier.create(spyPaperChannel.notifyUploadAsync(file1, getInputStream(), "1122"))
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

    @Test
    @DisplayName("whenCreateTenderWithEndDateBeforeStartDateThrowException")
    void createTenderWithBadlyFormatDateTest(){
        TenderCreateRequestDTO dto = getTenderDTO();
        Instant passed = Instant.now().minus(20, ChronoUnit.DAYS);
        dto.setEndDate(new Date(passed.toEpochMilli()));
        try {
            this.paperChannelService.createOrUpdateTender(dto).block();
            fail("Control date not working");
        } catch (PnGenericException ex) {
            assertEquals(BADLY_DATE_INTERVAL, ex.getExceptionType());
        }
    }

    @Test
    @DisplayName("whenCreateTenderOkThenReturnResponse")
    void createTenderOK(){
        TenderCreateRequestDTO dto = getTenderDTO();

        Mockito.when(this.tenderDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(getListTender(1).get(0)));

        TenderCreateResponseDTO response = this.paperChannelService.createOrUpdateTender(dto).block();
        assertNotNull(response);
        assertEquals(TenderCreateResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertTrue(response.getResult());
    }


    @Test
    @DisplayName("whenCreateDriverButTenderNotExistedThenThrowError")
    void createDriverWithNoTender(){
        DeliveryDriverDTO dto = getDriverDTO();

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.createOrUpdateDriver("1234", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(TENDER_NOT_EXISTED, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCreateDriverOkThenReturnResponse")
    void createDriverOK(){
        DeliveryDriverDTO dto = getDriverDTO();

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(getListTender(1).get(0)));

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(getListDrivers(1, true).get(0)));

        this.paperChannelService.createOrUpdateDriver("1224", dto).block();
        Mockito.verify(this.deliveryDriverDAO, Mockito.times(1))
                .createOrUpdate(Mockito.any());

        Mockito.verify(this.tenderDAO, Mockito.times(1))
                .getTender(Mockito.any());

        Mockito.verify(this.deliveryDriverDAO, Mockito.times(1))
                .getDeliveryDriver(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("whenCreateCostWithoutZoneOrCapThenThrowError")
    void createCostWithoutZoneOrCap(){
        CostDTO dto = getCostDTO();

        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(COST_BADLY_CONTENT, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();

        dto.setCap(new ArrayList<>());

        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(COST_BADLY_CONTENT, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCreateCostWithoutDriverThenThrowError")
    void createCostWithoutDriver(){
        CostDTO dto = getCostDTO();
        dto.setZone(InternationalZoneEnum._1);

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                        .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_NOT_EXISTED, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();

    }

    @Test
    @DisplayName("whenCreateCostThenReturnResponse")
    void createCostOK(){
        CostDTO dto = getCostDTO();
        dto.setZone(InternationalZoneEnum._1);

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(Mockito.any(), Mockito.any(), Mockito.any()))
                        .thenReturn(Flux.empty());

        Mockito.when(costDAO.createOrUpdate(Mockito.any()))
                        .thenReturn(Mono.just(getCost("ZONE_1", null, "AR")));

        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_NOT_EXISTED, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();

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
            tender.setDescription("GARA_2023");

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

    private InputStream getInputStream(){
        return new InputStream() {
            private final byte[] msg = "Hello World".getBytes();
            private int index = 0;
            @Override
            public int read() {
                if (index >= msg.length) {
                    return -1;
                }
                return msg[index++];
            }
        };
    }

    private TenderCreateRequestDTO getTenderDTO(){
        TenderCreateRequestDTO dto = new TenderCreateRequestDTO();
        dto.setCode("1234444");
        dto.setStartDate(new Date());
        dto.setEndDate(new Date());
        return dto;
    }

    private DeliveryDriverDTO getDriverDTO(){
        DeliveryDriverDTO dto = new DeliveryDriverDTO();
        dto.setTaxId("12345678901");
        dto.setFsu(true);
        dto.setDenomination("GLS");
        dto.setBusinessName("GLS");
        return dto;
    }

    private CostDTO getCostDTO(){
        CostDTO dto = new CostDTO();
        dto.setPrice(1.23F);
        dto.setPriceAdditional(1.23F);
        dto.setDriverCode("1223444");
        dto.setTenderCode("1233344");
        return dto;
    }

}