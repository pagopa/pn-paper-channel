package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.rest.v1.dto.AttachmentDetails;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTimeUtils;

import java.util.Date;

@Slf4j
public class AttachmentMapper {

    private AttachmentMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    private static final BaseMapper<PnAttachmentInfo,AttachmentInfo> mapper = new BaseMapperImpl<>(PnAttachmentInfo.class,AttachmentInfo.class);

    public static AttachmentInfo fromSafeStorage(FileDownloadResponseDto response){
        AttachmentInfo info = new AttachmentInfo();
        info.setFileKey(response.getKey());
        if (response.getDownload() != null && response.getDownload().getUrl() != null){
            info.setUrl(response.getDownload().getUrl());
        }
        info.setDocumentType(response.getDocumentType());
        return info;
    }

    public static AttachmentInfo fromEntity(PnAttachmentInfo entity){
        return mapper.toDTO(entity);
    }

    public static PnAttachmentInfo toEntity(AttachmentInfo dto){
        return mapper.toEntity(dto);
    }

    public static AttachmentDetails toAttachmentDetails(PnAttachmentInfo attachments){
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setId(attachments.getId());
        attachmentDetails.setDocumentType(attachments.getDocumentType());
        attachmentDetails.setUrl(attachments.getUrl());
        attachmentDetails.setDate(DateUtils.parseDateString(attachments.getDate()));
        return attachmentDetails;
    }

    public static AttachmentDetails fromAttachmentDetailsDto(AttachmentDetailsDto attachments){
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setId(attachments.getId());
        attachmentDetails.setDocumentType(attachments.getDocumentType());
        attachmentDetails.setUrl(attachments.getUrl());
        attachmentDetails.setDate(DateUtils.getDatefromOffsetDateTime(attachments.getDate()));
        return attachmentDetails;
    }

}
