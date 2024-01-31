package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;


@Component
@RequiredArgsConstructor
@CustomLog
public class SafeStorageClientImpl implements SafeStorageClient {

    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;
    private final WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @PostConstruct
    public void initWebClient() {
        webClient = webClientBuilder.build();
    }


    @Override
    public Mono<FileDownloadResponseDto> getFile(String fileKey) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage getFile";
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION, null);
        String reqFileKey = fileKey;
        log.info("Getting file with {} key", fileKey);
        final String BASE_URL = "safestorage://";
        if (fileKey.contains(BASE_URL)){
            fileKey = fileKey.replace(BASE_URL, "");
        }
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
    public Mono<FileCreationResponseDto> createFile(FileCreationWithContentRequest fileCreationRequestWithContent) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage createFile";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION);

        var fileCreationRequest = new FileCreationRequestDto();
        fileCreationRequest.setContentType(fileCreationRequestWithContent.getContentType());
        fileCreationRequest.setDocumentType(fileCreationRequestWithContent.getDocumentType());
        fileCreationRequest.setStatus(fileCreationRequestWithContent.getStatus());

        return fileUploadApi.createFile(pnPaperChannelConfig.getSafeStorageCxId(), fileCreationRequest )
                .doOnError( res -> log.error("File creation error - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequestWithContent.getContent().length));
    }

    @Override
    public Mono<Void> uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponseDto fileCreationResponse, String sha256) {
        final String UPLOAD_FILE_CONTENT = "Safe Storage uploadContent";
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, UPLOAD_FILE_CONTENT, fileCreationResponse.getKey());

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-type", fileCreationRequest.getContentType());
        headers.add("x-amz-checksum-sha256", sha256);
        headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

        URI url = URI.create(fileCreationResponse.getUploadUrl());
        HttpMethod method = fileCreationResponse.getUploadMethod() == FileCreationResponseDto.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;

        return webClient.method(method)
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(BodyInserters.fromResource(new ByteArrayResource(fileCreationRequest.getContent())))
                .retrieve()
                .toEntity(String.class)
                .flatMap(stringResponseEntity -> {
                    if (stringResponseEntity.getStatusCodeValue() != org.springframework.http.HttpStatus.OK.value()) {
                        return Mono.error(new RuntimeException());
                    }
                    return Mono.empty();
                });
    }


}
