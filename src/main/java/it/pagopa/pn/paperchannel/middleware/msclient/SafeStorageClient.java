package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import reactor.core.publisher.Mono;

public interface SafeStorageClient {

    Mono<FileDownloadResponseDto> getFile(String fileKey);
    Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequestWithContent, String sha256);
}
