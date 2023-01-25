package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.rest.v1.dto.InfoDownloadDTO;

public class FileMapper {

    private FileMapper(){
        throw new IllegalCallerException();
    }

    public static InfoDownloadDTO toDownloadFile(PnDeliveryFile pnDeliveryFile){
        InfoDownloadDTO dto = new InfoDownloadDTO();
        dto.setUuid(pnDeliveryFile.getUuid());
        dto.setUrl(pnDeliveryFile.getUrl());
        dto.setStatus(InfoDownloadDTO.StatusEnum.fromValue(pnDeliveryFile.getStatus()));
        dto.setRetryAfter((pnDeliveryFile.getStatus().equals("UPLOADED"))?10L:null);
        return dto;
    }
}
