package it.pagopa.pn.paperchannel.s3;

import reactor.core.publisher.Mono;

import java.io.File;

public interface S3Bucket {

    Mono<String> presignedUrl();
    Mono<String> putObject(File file);

}
