package it.pagopa.pn.paperchannel.service.tenders;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.InstanceCreator;
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
import it.pagopa.pn.paperchannel.rest.v1.dto.InfoDownloadDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.NotifyResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.NotifyUploadRequestDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PresignedUrlResponseDto;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.InputStream;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXCEL_BADLY_CONTENT;
import static org.junit.jupiter.api.Assertions.*;

class FileDownloadAndUploadFromServiceTest extends BaseTest {


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

    private PnDeliveryFile fileUploading;

    @BeforeEach
    void setUp(){
        fileUploading = InstanceCreator.getPnDeliveryFile(InfoDownloadDTO.StatusEnum.UPLOADING.toString());
        Mockito.when(fileDownloadDAO.create(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

        Mockito.when(fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

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

        PresignedUrlResponseDto response = this.paperChannelService.getPresignedUrl().block();
        assertNotNull(response);
        assertEquals(mocked.getUuid(), response.getUuid());
        assertEquals(mocked.getPresignedUrl(), response.getPresignedUrl());
    }

    @Test
    @DisplayName("whenDownloadExcelFirstRequestAndFileNotReady")
    void downloadExcelTenderFirstRequestAndFileNotReady(){
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
        fileUploading.setStatus(FileStatusCodeEnum.ERROR.getCode());
        fileUploading.setErrorMessage(errorMessage);

        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

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
                .thenReturn(Mono.just(fileUploading));

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

        fileUploading.setStatus(FileStatusCodeEnum.UPLOADED.getCode());


        Mockito.when(this.fileDownloadDAO.getUuid(Mockito.any()))
                .thenReturn(Mono.just(fileUploading));

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
    void notifyUploadWithErrorMappingFromExcel(){
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
