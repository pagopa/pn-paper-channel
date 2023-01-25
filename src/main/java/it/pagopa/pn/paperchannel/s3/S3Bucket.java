package it.pagopa.pn.paperchannel.s3;

import reactor.core.publisher.Mono;

public interface S3Bucket {

    Mono<String> presignedUrl();

}
