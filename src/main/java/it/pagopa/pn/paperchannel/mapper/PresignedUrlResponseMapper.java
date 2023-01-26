package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.rest.v1.dto.PresignedUrlResponseDto;
import org.apache.commons.lang3.StringUtils;

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

}
