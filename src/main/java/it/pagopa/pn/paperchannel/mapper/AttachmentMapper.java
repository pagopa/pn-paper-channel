package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.AttachmentInfoEntity;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;

public class AttachmentMapper {

    private AttachmentMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    private static final BaseMapper<AttachmentInfoEntity,AttachmentInfo> mapper = new BaseMapperImpl<>(AttachmentInfoEntity.class,AttachmentInfo.class);

    public static AttachmentInfo fromSafeStorage(FileDownloadResponseDto response){
        AttachmentInfo info = new AttachmentInfo();
        info.setFileKey(response.getKey());
        if (response.getDownload() != null && response.getDownload().getUrl() != null){
            info.setUrl(response.getDownload().getUrl());
        }
        info.setDocumentType(response.getDocumentType());
        return info;
    }

    public static AttachmentInfo fromEntity(AttachmentInfoEntity entity){

        return mapper.toDTO(entity);
    }

    public static AttachmentInfoEntity toEntity(AttachmentInfo entity){

        return mapper.toEntity(entity);
    }
}
