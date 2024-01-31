package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import reactor.core.publisher.Mono;

public interface SafeStorageClient {

    Mono<FileDownloadResponseDto> getFile(String fileKey);
    Mono<FileCreationResponseDto> createFile(FileCreationWithContentRequest fileCreationRequestWithContent);
    Mono<Void> uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponseDto fileCreationResponse, String sha256);
}
