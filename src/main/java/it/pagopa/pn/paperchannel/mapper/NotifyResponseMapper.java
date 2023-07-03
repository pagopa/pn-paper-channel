package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.NotifyResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;

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
        dto.setRetryAfter((pnDeliveryFile.getStatus().equals(FileStatusCodeEnum.UPLOADING.getCode()) ||
                pnDeliveryFile.getStatus().equals(FileStatusCodeEnum.IN_PROGRESS.getCode()))?BigDecimal.valueOf(100L):null);
        return dto;
    }
}
