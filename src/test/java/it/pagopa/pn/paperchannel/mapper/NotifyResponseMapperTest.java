package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorMessage;
import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.NotifyResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotifyResponseMapperTest {
    PnDeliveryFile pnDeliveryFileStatusComplete;
    PnDeliveryFile pnDeliveryFileStatusUploaded;
    PnDeliveryFile pnDeliveryFileStatusUploading;
    PnDeliveryFile pnDeliveryFileStatusInProgress;
    PnDeliveryFile pnDeliveryFileStatusError;
    @BeforeEach
    void setUp(){
        this.initialize();
    }
    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<NotifyResponseMapper> constructor = NotifyResponseMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        assertNull(exception.getMessage());
    }

    @Test
    void toDtoCompleteTest(){
        NotifyResponseDto responseDto = NotifyResponseMapper.toDto(pnDeliveryFileStatusComplete);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(pnDeliveryFileStatusComplete.getStatus(), FileStatusCodeEnum.COMPLETE.getCode());
    }

    @Test
    void toDtoInProgressTest(){
        NotifyResponseDto responseDto = NotifyResponseMapper.toDto(pnDeliveryFileStatusInProgress);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(pnDeliveryFileStatusInProgress.getStatus(), FileStatusCodeEnum.IN_PROGRESS.getCode());
    }
    @Test
    void toDtoErrorTest(){
        NotifyResponseDto responseDto = NotifyResponseMapper.toDto(pnDeliveryFileStatusError);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(pnDeliveryFileStatusError.getStatus(), FileStatusCodeEnum.ERROR.getCode());
    }

    @Test
    void toDtoUploadedTest(){
        NotifyResponseDto responseDto = NotifyResponseMapper.toDto(pnDeliveryFileStatusUploaded);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(pnDeliveryFileStatusUploaded.getStatus(), FileStatusCodeEnum.UPLOADED.getCode());
    }

    @Test
    void toDtoUploadingTest(){
        NotifyResponseDto responseDto = NotifyResponseMapper.toDto(pnDeliveryFileStatusUploading);
        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(pnDeliveryFileStatusUploading.getStatus(), FileStatusCodeEnum.UPLOADING.getCode());
    }

    private void initialize(){
        pnDeliveryFileStatusComplete = new PnDeliveryFile();
        pnDeliveryFileStatusInProgress = new PnDeliveryFile();
        pnDeliveryFileStatusError = new PnDeliveryFile();
        pnDeliveryFileStatusUploaded = new PnDeliveryFile();
        pnDeliveryFileStatusUploading = new PnDeliveryFile();

        pnDeliveryFileStatusComplete.setStatus(FileStatusCodeEnum.COMPLETE.getCode());
        pnDeliveryFileStatusComplete.setUuid("uuid");
        pnDeliveryFileStatusComplete.setFilename("filename");
        pnDeliveryFileStatusComplete.setUrl("url");
        PnErrorMessage errorMessage = new PnErrorMessage();
        List<PnErrorDetails> errorDetailsList = new ArrayList<>();
        PnErrorDetails errorDetails = new PnErrorDetails();
        errorDetails.setRow(1);
        errorDetails.setCol(1);
        errorDetails.setColName("colName");
        errorDetails.setMessage("message");
        errorDetailsList.add(errorDetails);
        errorMessage.setMessage("errorMessage");
        errorMessage.setErrorDetails(errorDetailsList);
        pnDeliveryFileStatusComplete.setErrorMessage(errorMessage);


        pnDeliveryFileStatusInProgress.setStatus(FileStatusCodeEnum.IN_PROGRESS.getCode());
        pnDeliveryFileStatusInProgress.setUuid("uuid");
        pnDeliveryFileStatusInProgress.setFilename("filename");
        pnDeliveryFileStatusInProgress.setUrl("url");
        PnErrorMessage errorMessage1 = new PnErrorMessage();
        List<PnErrorDetails> errorDetailsList1 = new ArrayList<>();
        PnErrorDetails errorDetails1 = new PnErrorDetails();
        errorDetails1.setRow(1);
        errorDetails1.setCol(1);
        errorDetails1.setColName("colName");
        errorDetails1.setMessage("message");
        errorDetailsList1.add(errorDetails1);
        errorMessage1.setMessage("errorMessage");
        errorMessage1.setErrorDetails(errorDetailsList1);
        pnDeliveryFileStatusInProgress.setErrorMessage(errorMessage1);


        pnDeliveryFileStatusError.setStatus(FileStatusCodeEnum.ERROR.getCode());
        pnDeliveryFileStatusError.setUuid("uuid");
        pnDeliveryFileStatusError.setFilename("filename");
        pnDeliveryFileStatusError.setUrl("url");
        PnErrorMessage errorMessage2 = new PnErrorMessage();
        List<PnErrorDetails> errorDetailsList2 = new ArrayList<>();
        PnErrorDetails errorDetails2 = new PnErrorDetails();
        errorDetails2.setRow(1);
        errorDetails2.setCol(1);
        errorDetails2.setColName("colName");
        errorDetails2.setMessage("message");
        errorDetailsList2.add(errorDetails2);
        errorMessage2.setMessage("errorMessage");
        errorMessage2.setErrorDetails(errorDetailsList2);
        pnDeliveryFileStatusError.setErrorMessage(errorMessage2);


        pnDeliveryFileStatusUploaded.setStatus(FileStatusCodeEnum.UPLOADED.getCode());
        pnDeliveryFileStatusUploaded.setUuid("uuid");
        pnDeliveryFileStatusUploaded.setFilename("filename");
        pnDeliveryFileStatusUploaded.setUrl("url");
        PnErrorMessage errorMessage3 = new PnErrorMessage();
        List<PnErrorDetails> errorDetailsList3 = new ArrayList<>();
        PnErrorDetails errorDetails3 = new PnErrorDetails();
        errorDetails3.setRow(1);
        errorDetails3.setCol(1);
        errorDetails3.setColName("colName");
        errorDetails3.setMessage("message");
        errorDetailsList3.add(errorDetails3);
        errorMessage3.setMessage("errorMessage");
        errorMessage3.setErrorDetails(errorDetailsList3);
        pnDeliveryFileStatusUploaded.setErrorMessage(errorMessage3);

        pnDeliveryFileStatusUploading.setStatus(FileStatusCodeEnum.UPLOADING.getCode());
        pnDeliveryFileStatusUploading.setUuid("uuid");
        pnDeliveryFileStatusUploading.setFilename("filename");
        pnDeliveryFileStatusUploading.setUrl("url");
        PnErrorMessage errorMessage4 = new PnErrorMessage();
        List<PnErrorDetails> errorDetailsList4 = new ArrayList<>();
        PnErrorDetails errorDetails4 = new PnErrorDetails();
        errorDetails4.setRow(1);
        errorDetails4.setCol(1);
        errorDetails4.setColName("colName");
        errorDetails4.setMessage("message");
        errorDetailsList4.add(errorDetails4);
        errorMessage4.setMessage("errorMessage");
        errorMessage4.setErrorDetails(errorDetailsList4);
        pnDeliveryFileStatusUploading.setErrorMessage(errorMessage4);
    }
}