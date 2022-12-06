package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.pojo.AttachmentInfo;

public class AttachmentMapper {

    private AttachmentMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static AttachmentInfo fromSafeStorage(FileDownloadResponseDto response){
        AttachmentInfo info = new AttachmentInfo();
        info.setId(response.getKey());
        if (response.getDownload() != null && response.getDownload().getUrl() != null){
            info.setUrl(response.getDownload().getUrl());
        }
        info.setDocumentType(response.getDocumentType());
        return info;
    }



}
