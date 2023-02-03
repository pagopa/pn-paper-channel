package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.rest.v1.dto.InfoDownloadDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.NotifyResponseDto;

import java.math.BigDecimal;

public class NotifyResponseMapper {
    private NotifyResponseMapper(){
        throw new IllegalCallerException();
    }
    public static NotifyResponseDto toUploadFile(PnDeliveryFile pnDeliveryFile){
        NotifyResponseDto dto = new NotifyResponseDto();
        dto.setUuid(pnDeliveryFile.getUuid());
        dto.setStatus(NotifyResponseDto.StatusEnum.fromValue(pnDeliveryFile.getStatus()));
        dto.setRetryAfter((pnDeliveryFile.getStatus().equals("UPLOADING"))?BigDecimal.valueOf(10L):null);
        return dto;
    }
}
