package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadInfoDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.AttachmentDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.OffsetDateTime;

class AttachmentMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<AttachmentMapper> constructor = AttachmentMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void attachmentMapperToAttachmentDetailsTest() {
        AttachmentDetails attachmentDetails=AttachmentMapper.toAttachmentDetails(getAttachments());
        Assertions.assertNotNull(attachmentDetails);
    }
    @Test
    void attachmentMapperFromAttachmentDetailsDtoTest() {
        AttachmentDetails attachmentDetails=AttachmentMapper.fromAttachmentDetailsDto(getAttachmentsDetails());
        Assertions.assertNotNull(attachmentDetails);
    }

    @Test
    void attachmentMapperFromSafeStorageTest() {
        AttachmentInfo attachmentInfo=AttachmentMapper.fromSafeStorage(getFileDownloadResponseDto());
        Assertions.assertNotNull(attachmentInfo);
    }

    @Test
    void attachmentMapperFromEntityTest() {
        AttachmentInfo attachmentInfo=AttachmentMapper.fromEntity(new PnAttachmentInfo());
        Assertions.assertNotNull(attachmentInfo);
    }
    @Test
    void attachmentMapperToEntityTest() {
        PnAttachmentInfo attachmentInfo=AttachmentMapper.toEntity(new AttachmentInfo());
        Assertions.assertNotNull(attachmentInfo);
    }

    private FileDownloadResponseDto getFileDownloadResponseDto(){
        FileDownloadResponseDto response= new FileDownloadResponseDto();
        FileDownloadInfoDto fileDownloadInfoDto= new FileDownloadInfoDto();
        fileDownloadInfoDto.setUrl("www.google.com");
        response.setKey("12345");
        response.setDownload(fileDownloadInfoDto);
        response.setDocumentStatus("ok");
        response.setDocumentType("pdf");
        return response;
    }
    private PnAttachmentInfo getAttachments(){
        PnAttachmentInfo dto = new PnAttachmentInfo();
        dto.setId("12345");
        dto.setDocumentType("pdf");
        dto.setUrl("http://localhost:8080");
        return dto;
    }
    private AttachmentDetailsDto getAttachmentsDetails(){
        AttachmentDetailsDto dto = new AttachmentDetailsDto();
        dto.setDate(OffsetDateTime.now());
        dto.setId("12345");
        dto.setDocumentType("pdf");
        dto.setUrl("http://localhost:8080");
        return dto;
    }
}
