package it.pagopa.pn.paperchannel.service.tenders;

import it.pagopa.pn.paperchannel.config.InstanceCreator;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InfoDownloadDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.NotifyResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.NotifyUploadRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PresignedUrlResponseDto;
import it.pagopa.pn.paperchannel.mapper.DeliveryDriverMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileDownloadAndUploadFromServiceTest {

    @InjectMocks
    private PaperChannelServiceImpl paperChannelService;
    @Mock
    private CostDAO costDAO;
    @Mock
    private DeliveryDriverDAO deliveryDriverDAO;
    @Mock
    private ExcelDAO<DeliveriesData> excelDAO;
    @Mock
    private FileDownloadDAO fileDownloadDAO;
    @Mock
    private S3Bucket s3Bucket;
    private MockedStatic<DeliveryDriverMapper> mockedStaticDelivery;

    private PnDeliveryFile fileUploading;
    private NotifyUploadRequestDto notifyUploadRequestDto;

    @BeforeEach
    void setUp(){
        notifyUploadRequestDto = new NotifyUploadRequestDto();
        notifyUploadRequestDto.setUuid("UUID_TEST");

        fileUploading = InstanceCreator.getPnDeliveryFile(InfoDownloadDTO.StatusEnum.UPLOADING.toString());

//        Mockito.when(fileDownloadDAO.create(Mockito.any()))
//                .thenReturn(Mono.just(fileUploading));

//        Mockito.when(fileDownloadDAO.getUuid(Mockito.any()))
//                .thenReturn(Mono.just(fileUploading));

    }

    @AfterEach
    void after(){
        if (mockedStaticDelivery != null){
            mockedStaticDelivery.close();
        }
    }

    @Test
    @DisplayName("whenRetrievePresignedUrlThenResponse")
    void getPresignedUrlOK(){
        PresignedUrlResponseDto mocked = getPresignedDTO();
        Mockito.when(s3Bucket.presignedUrl()).thenReturn(Mono.just(mocked));
        Mockito.when(fileDownloadDAO.create(Mockito.any())).thenReturn(Mono.just(fileUploading));

        PresignedUrlResponseDto response = this.paperChannelService.getPresignedUrl().block();
        assertNotNull(response);
        assertEquals(mocked.getUuid(), response.getUuid());
        assertEquals(mocked.getPresignedUrl(), response.getPresignedUrl());
    }

    @Test
    @DisplayName("whenDownloadExcelFirstRequestAndFileNotReady")
    void downloadExcelTenderFirstRequestAndFileNotReady(){
        Mockito.when(fileDownloadDAO.create(Mockito.any())).thenReturn(Mono.just(fileUploading));
        InfoDownloadDTO response = this.paperChannelService.downloadTenderFile("1234", null).block();
        assertNotNull(response);
        assertEquals(fileUploading.getStatus(), response.getStatus().getValue());
        assertEquals(fileUploading.getUuid(), response.getUuid());
        assertNull(response.getData());
        assertNotNull(response.getRetryAfter());
    }

    @Test
    @DisplayName("whenDownloadExcelSecondRequestAndFileNotReady")
    void downloadExcelTenderSecondRequestAndFileNotReady(){
        Mockito.when(s3Bucket.getObjectData(Mockito.any())).thenReturn(null);
        Mockito.when(fileDownloadDAO.getUuid(Mockito.any())).thenReturn(Mono.just(fileUploading));
        InfoDownloadDTO response = this.paperChannelService.downloadTenderFile("1234", "UUID_FILE").block();
        assertNotNull(response);
        assertEquals(fileUploading.getStatus(), response.getStatus().getValue());
        assertEquals(fileUploading.getUuid(), response.getUuid());
        assertNull(response.getData());
        assertNotNull(response.getRetryAfter());
    }

    @Test
    @DisplayName("whenDownloadExcelThirdRequestAndFileReady")
    void downloadExcelTenderThirdRequestAndFileReady(){
        fileUploading.setStatus(InfoDownloadDTO.StatusEnum.UPLOADED.getValue());

        Mockito.when(fileDownloadDAO.getUuid(Mockito.any())).thenReturn(Mono.just(fileUploading));
        Mockito.when(s3Bucket.getObjectData(Mockito.any())).thenReturn("Byte mock".getBytes());

        InfoDownloadDTO response = this.paperChannelService.downloadTenderFile("1234", "UUID_FILE").block();

        assertNotNull(response);
        assertEquals(fileUploading.getStatus(), response.getStatus().getValue());
        assertEquals(fileUploading.getUuid(), response.getUuid());
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
    void notifyUploadSyncWithoutUUID(){
        StepVerifier.create(this.paperChannelService.notifyUpload("1234", new NotifyUploadRequestDto()))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(BADLY_REQUEST, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCalledNotifyUploadButFileRequestNotExistedThenThrowException")
    void notifyUploadSyncButFileNotExisted(){

        Mockito.when(this.fileDownloadDAO.getUuid(notifyUploadRequestDto.getUuid()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.notifyUpload("1234", notifyUploadRequestDto))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(FILE_REQUEST_ASYNC_NOT_FOUND, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusInErrorThrowException")
    void notifyUploadSyncWithFileStatusInError(){
        PnErrorMessage errorMessage = new PnErrorMessage();
        errorMessage.setMessage("Error With file");
        fileUploading.setStatus(FileStatusCodeEnum.ERROR.getCode());
        fileUploading.setErrorMessage(errorMessage);

        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

        StepVerifier.create(this.paperChannelService.notifyUpload("1234", notifyUploadRequestDto))
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
                .thenReturn(Mono.just(fileUploading));

        StepVerifier.create(this.paperChannelService.notifyUpload("1234", notifyUploadRequestDto))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnExcelValidatorException);
                    assertEquals(BADLY_REQUEST, ((PnExcelValidatorException) e).getErrorType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusIsUploadedThenResponse")
    void notifyUploadSyncWithFileStatusInUploaded(){

        fileUploading.setStatus(FileStatusCodeEnum.UPLOADED.getCode());


        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

        NotifyResponseDto dto = this.paperChannelService.notifyUpload("1234", notifyUploadRequestDto).block();
        assertNotNull(dto);
        assertNull(dto.getRetryAfter());
        assertEquals(NotifyResponseDto.StatusEnum.COMPLETE, dto.getStatus());
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusIsUploadingAndS3NotHaveAFileThenResponseRetryAfter")
    void notifyUploadSyncWithFileStatusInUploadingAndS3NoFIle(){
        Mockito.when(s3Bucket.getFileInputStream(Mockito.any()))
                .thenReturn(null);

        Mockito.when(fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

        NotifyResponseDto dto = this.paperChannelService.notifyUpload("1234", notifyUploadRequestDto).block();
        assertNotNull(dto);
        assertNotNull(dto.getRetryAfter());
        assertEquals(NotifyResponseDto.StatusEnum.IN_PROGRESS, dto.getStatus());
    }

    @Test
    @DisplayName("whenCalledNotifyUploadWithFileStatusIsUploadingAndS3HaveAFileThenStartNotifyAsync")
    void notifyUploadSyncWithFileStatusInUploadingAndS3haveFile(){
        PaperChannelServiceImpl spyPaperChannelService = Mockito.spy(this.paperChannelService);

        Mockito.when(fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

        fileUploading.setStatus(FileStatusCodeEnum.IN_PROGRESS.getCode());

        NotifyResponseDto dto = spyPaperChannelService.notifyUpload("1234", notifyUploadRequestDto).block();
        assertNotNull(dto);

        assertNotNull(dto.getRetryAfter());
        assertEquals(NotifyResponseDto.StatusEnum.IN_PROGRESS, dto.getStatus());
    }


    @Test
    @DisplayName("whenCallNotifyAsyncWithExcelBadlyContentThenSaveFileRequestWithError")
    void notifyUploadAsyncWithErrorExcelValidation(){
        fileUploading.setStatus(FileStatusCodeEnum.UPLOADED.getCode());

        Mockito.when(this.excelDAO.readData(Mockito.any()))
                .thenThrow(new PnGenericException(EXCEL_BADLY_CONTENT, "ERROR VALIDATION"));

        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

        StepVerifier.create(this.paperChannelService.notifyUploadAsync(fileUploading, getInputStream(), "1122"))
                .expectError(PnGenericException.class)
                .verify();
    }

    @Test
    @DisplayName("whenCallNotifyAsyncWithBadlyMappingThenThrowException")
    void notifyUploadAsyncWithErrorMappingFromExcel(){
        fileUploading.setStatus(FileStatusCodeEnum.UPLOADED.getCode());

        Mockito.when(this.excelDAO.readData(Mockito.any()))
                .thenReturn(new DeliveriesData());

        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

        mockedStaticDelivery = Mockito.mockStatic(DeliveryDriverMapper.class);
        mockedStaticDelivery.when(() -> {
            DeliveryDriverMapper.toEntityFromExcel(Mockito.any(), Mockito.any());
        }).thenThrow(new PnExcelValidatorException(ExceptionTypeEnum.INVALID_CAP_PRODUCT_TYPE, null));

        StepVerifier.create(this.paperChannelService.notifyUploadAsync(fileUploading, getInputStream(), "1122"))
                .expectError(PnExcelValidatorException.class)
                .verify();
    }

    @Test
    @DisplayName("whenCallNotifyAsyncWithExceptionCreateDriverThenThrowException")
    void notifyUploadAsyncErrorWithCreateDriver(){
        //MOCK EXCEL DAO READ DATA
        Mockito.when(this.excelDAO.readData(Mockito.any()))
                .thenReturn(new DeliveriesData());

        //MOCK MAPPER AND VALIDATOR
        Map<PnDeliveryDriver, List<PnCost>> map = new HashMap<>();
        map.put(InstanceCreator.getDriver(true), InstanceCreator.getAllNationalCost("1234", "1234", true));
        mockedStaticDelivery = Mockito.mockStatic(DeliveryDriverMapper.class);
        mockedStaticDelivery.when(() -> {
            DeliveryDriverMapper.toEntityFromExcel(Mockito.any(), Mockito.any());
        }).thenReturn(map);


        //MOCK GET ALL DRIVER FROM TENDER
        Mockito.when(deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(
                        Flux.fromStream(InstanceCreator.getListDriver(5).stream())
                );

        //MOCK DELETE ALL DRIVER
        PaperChannelServiceImpl spyPaperChannel = Mockito.spy(this.paperChannelService);
        Mockito.doReturn(Mono.just("").then()).when(spyPaperChannel).deleteDriver(Mockito.any(), Mockito.any());

        //MOCK CREATE DRIVER
        Mockito.when(this.deliveryDriverDAO.createOrUpdate(Mockito.any()))
                        .thenReturn(Mono.error(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage())));

        //MOCK UPDATE WITH ERROR
        fileUploading.setStatus(FileStatusCodeEnum.UPLOADED.getCode());
        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));


        StepVerifier.create(spyPaperChannel.notifyUploadAsync(fileUploading, getInputStream(), "1122"))
                .expectError(PnGenericException.class)
                .verify();
    }

    @Test
    @DisplayName("whenCallNotifyAsyncWithCorrectDataThenUpdateStatus")
    void notifyUploadAsyncWithCorrectDataThenUpdateStatus(){
        //MOCK EXCEL DAO READ DATA
        Mockito.when(this.excelDAO.readData(Mockito.any()))
                .thenReturn(new DeliveriesData());

        //MOCK MAPPER AND VALIDATOR
        Map<PnDeliveryDriver, List<PnCost>> map = new HashMap<>();
        map.put(InstanceCreator.getDriver(true), InstanceCreator.getAllNationalCost("1234", "1234", true));
        mockedStaticDelivery = Mockito.mockStatic(DeliveryDriverMapper.class);
        mockedStaticDelivery.when(() -> {
            DeliveryDriverMapper.toEntityFromExcel(Mockito.any(), Mockito.any());
        }).thenReturn(map);


        //MOCK GET ALL DRIVER FROM TENDER
        Mockito.when(deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(
                        Flux.fromStream(InstanceCreator.getListDriver(5).stream())
                );

        //MOCK DELETE ALL DRIVER
        PaperChannelServiceImpl spyPaperChannel = Mockito.spy(this.paperChannelService);
        Mockito.doReturn(Mono.just("").then()).when(spyPaperChannel).deleteDriver(Mockito.any(), Mockito.any());

        //MOCK CREATE DRIVER
        Mockito.when(deliveryDriverDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(InstanceCreator.getDriver(true)));

        //MOCK CREATE COST
        Mockito.when(costDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(InstanceCreator.getCost("1234", null, List.of("12332"), "AR")));

        //MOCK UPDATE WITH COMPLETE STATUS
        Mockito.when(fileDownloadDAO.create(Mockito.any())).thenReturn(Mono.just(fileUploading));

        StepVerifier.create(spyPaperChannel.notifyUploadAsync(fileUploading, getInputStream(), "1122"))
                .verifyComplete();
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

}
