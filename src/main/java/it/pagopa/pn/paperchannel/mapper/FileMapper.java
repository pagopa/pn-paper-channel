package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnFile;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.InfoDownloadDTO;

import java.nio.charset.StandardCharsets;

public class FileMapper {

    private FileMapper(){
        throw new IllegalCallerException();
    }

    public static InfoDownloadDTO toDownloadFile(PnFile pnFile){
        InfoDownloadDTO dto = new InfoDownloadDTO();
        dto.setUuid(pnFile.getUuid());
        dto.setUrl(pnFile.getUrl());
        dto.setStatus(InfoDownloadDTO.StatusEnum.fromValue(pnFile.getStatus()));
        dto.setRetryAfter((pnFile.getStatus().equals("UPLOADED"))?10L:null);
        return dto;
    }
}
