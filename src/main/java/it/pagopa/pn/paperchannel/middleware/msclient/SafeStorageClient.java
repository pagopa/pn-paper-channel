package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import reactor.core.publisher.Mono;

public interface SafeStorageClient {

    Mono<FileDownloadResponseDto> getFile(String fileKey);
}
