package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.NotifyResponseDto;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class NotifyResponseMapper {

    private NotifyResponseMapper(){
        throw new IllegalCallerException();
    }

    public static NotifyResponseDto toDto(PnDeliveryFile pnDeliveryFile){
        NotifyResponseDto dto = new NotifyResponseDto();
        dto.setUuid(pnDeliveryFile.getUuid());
        if (StringUtils.equalsIgnoreCase(pnDeliveryFile.getStatus(), FileStatusCodeEnum.COMPLETE.getCode())
            || StringUtils.equalsIgnoreCase(pnDeliveryFile.getStatus(), FileStatusCodeEnum.UPLOADED.getCode())) {
            dto.setStatus(NotifyResponseDto.StatusEnum.COMPLETE);
        } else if (StringUtils.equalsIgnoreCase(pnDeliveryFile.getStatus(), FileStatusCodeEnum.IN_PROGRESS.getCode())
                    || StringUtils.equalsIgnoreCase(pnDeliveryFile.getStatus(), FileStatusCodeEnum.UPLOADING.getCode())) {
            dto.setStatus(NotifyResponseDto.StatusEnum.IN_PROGRESS);
        } else if (StringUtils.equalsIgnoreCase(pnDeliveryFile.getStatus(), FileStatusCodeEnum.ERROR.getCode())) {
            dto.setStatus(NotifyResponseDto.StatusEnum.ERROR);
        }
        dto.setRetryAfter((pnDeliveryFile.getStatus().equals(FileStatusCodeEnum.UPLOADING.getCode()))?BigDecimal.valueOf(10L):null);
        return dto;
    }
}
