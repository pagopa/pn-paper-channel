package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import lombok.extern.slf4j.Slf4j;

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
        PnAttachmentInfo entity =  mapper.toEntity(dto);
        log.info("Mapper Entity : {}", entity.getUrl());
        return entity;
    }
}
