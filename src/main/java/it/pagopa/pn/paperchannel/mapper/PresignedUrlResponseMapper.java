package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PresignedUrlResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;


import java.net.URL;

public class PresignedUrlResponseMapper {

    private PresignedUrlResponseMapper() {
        throw new IllegalCallerException();
    }

    public static PresignedUrlResponseDto fromResult(URL url, String uuid){
        PresignedUrlResponseDto presignedUrlResponseDto = new PresignedUrlResponseDto();
        if (url != null) {
            presignedUrlResponseDto.setPresignedUrl(url.toString());
        }
        presignedUrlResponseDto.setUuid(uuid);
        return presignedUrlResponseDto;
    }

    public static PnDeliveryFile toEntity(PresignedUrlResponseDto dto){
        PnDeliveryFile pnDeliveryFile = new PnDeliveryFile();
        pnDeliveryFile.setUrl(dto.getPresignedUrl());
        pnDeliveryFile.setStatus(FileStatusCodeEnum.UPLOADING.getCode());
        pnDeliveryFile.setUuid(dto.getUuid());
        return pnDeliveryFile;
    }

}
