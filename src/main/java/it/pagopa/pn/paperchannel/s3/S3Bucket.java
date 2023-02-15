package it.pagopa.pn.paperchannel.s3;

import it.pagopa.pn.paperchannel.rest.v1.dto.PresignedUrlResponseDto;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.InputStream;

public interface S3Bucket {
    String PREFIX_URL = "tender-";

    Mono<PresignedUrlResponseDto> presignedUrl();
    Mono<File> putObject(File file);
    byte[] getObjectData(String filename);
    InputStream getFileInputStream(String filename);

}
