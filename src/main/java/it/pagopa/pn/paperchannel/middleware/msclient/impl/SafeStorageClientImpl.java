package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage.model.FileCreationRequest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage_reactive.api.FileUploadApi;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;


@Component
@RequiredArgsConstructor
@CustomLog
public class SafeStorageClientImpl implements SafeStorageClient {

    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;


    @Override
    public Mono<FileDownloadResponseDto> getFile(String fileKey) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage getFile";
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION, null);
        String reqFileKey = fileKey;
        log.info("Getting file with {} key", fileKey);
        fileKey = AttachmentsConfigUtils.cleanFileKey(fileKey);
        log.debug("Req params : {}", fileKey);

        return fileDownloadApi.getFile(fileKey, this.pnPaperChannelConfig.getSafeStorageCxId(), false)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .map(response -> {
                    if(response.getDownload() != null && response.getDownload().getRetryAfter() != null) {
                        throw new PnRetryStorageException(response.getDownload().getRetryAfter());
                    }
                    response.setKey(reqFileKey);
                    return response;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(ex);
                });
    }

    @Override
    public Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequestWithContent, String sha256) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage createFile";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION);

        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType(fileCreationRequestWithContent.getContentType());
        fileCreationRequest.setDocumentType(fileCreationRequestWithContent.getDocumentType());
        fileCreationRequest.setStatus(fileCreationRequestWithContent.getStatus());

        return fileUploadApi.createFile( pnPaperChannelConfig.getSafeStorageCxId(),"SHA-256", sha256,  fileCreationRequest )
                .doOnError( res -> log.error("File creation error - documentType={} filesize={} sha256={}", fileCreationRequest.getDocumentType(), fileCreationRequestWithContent.getContent().length, sha256));
    }


}
