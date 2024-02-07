package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import it.pagopa.pn.paperchannel.service.SafeStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class SafeStorageServiceImpl implements SafeStorageService {


    private final SafeStorageClient safeStorageClient;

    private final HttpConnector httpConnector;

    @Override
    public Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis) {
        if (n < 0)
            return Mono.error(new PnGenericException( DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage() ) );
        else {
            return Mono.delay(Duration.ofMillis( millis.longValue() ))
                    .flatMap(item -> safeStorageClient.getFile(fileKey)
                            .map(fileDownloadResponseDto -> fileDownloadResponseDto)
                            .onErrorResume(ex -> {
                                log.error ("Error with retrieve {}", ex.getMessage());
                                return Mono.error(ex);
                            })
                            .onErrorResume(PnRetryStorageException.class, ex ->
                                    getFileRecursive(n - 1, fileKey, ex.getRetryAfter())
                            ));
        }
    }

    @Override
    public Mono<byte[]> downloadFileAsByteArray(String url) {
        return httpConnector.downloadFileAsByteArray(url);
    }

    @Override
    public Mono<PDDocument> downloadFile(String url) {
        return httpConnector.downloadFile(url);
    }

    @Override
    public Mono<String> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
        log.info("Start createAndUploadFile - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

        String sha256 = computeSha256(fileCreationRequest.getContent());

        return safeStorageClient.createFile(fileCreationRequest, sha256)
                .onErrorResume(exception ->{
                    log.error("Cannot create file ", exception);
                    return Mono.error(new PnGenericException(ERROR_CODE_PAPERCHANNEL_ZIP_HANDLE, exception.getMessage()));
                })
                .flatMap(fileCreationResponse -> httpConnector.uploadContent(fileCreationRequest, fileCreationResponse, sha256).thenReturn(fileCreationResponse))
                .map(FileCreationResponse::getKey);
    }
}
