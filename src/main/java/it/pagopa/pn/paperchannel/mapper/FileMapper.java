package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InfoDownloadDTO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;


public class FileMapper {

    private FileMapper(){
        throw new IllegalCallerException();
    }

    public static InfoDownloadDTO toDownloadFile(PnDeliveryFile pnDeliveryFile, byte[] data){
        InfoDownloadDTO dto = new InfoDownloadDTO();
        dto.setUuid(pnDeliveryFile.getUuid());
        dto.setData(data);
        dto.setStatus(InfoDownloadDTO.StatusEnum.fromValue(pnDeliveryFile.getStatus()));
        dto.setRetryAfter((pnDeliveryFile.getStatus().equals("UPLOADING"))?100L:null);
        return dto;
    }
}
