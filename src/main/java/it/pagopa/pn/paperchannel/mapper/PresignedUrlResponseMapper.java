package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.rest.v1.dto.PresignedUrlResponseDto;
import org.apache.commons.lang3.StringUtils;

public class PresignedUrlResponseMapper {

    private PresignedUrlResponseMapper() {
        throw new IllegalCallerException();
    }

    public static PresignedUrlResponseDto fromResult(String data){
        PresignedUrlResponseDto presignedUrlResponseDto = new PresignedUrlResponseDto();
        if (StringUtils.isNotEmpty(data)) {
            presignedUrlResponseDto.setPresignedUrl(data);
        }
        return presignedUrlResponseDto;
    }

}
