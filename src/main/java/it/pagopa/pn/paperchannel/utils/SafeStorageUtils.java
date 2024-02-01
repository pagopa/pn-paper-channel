package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Duration;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ERROR_CODE_PAPERCHANNEL_UPLOADFILEERROR;

@Component
@Slf4j
@RequiredArgsConstructor
public class SafeStorageUtils {

    public static final String SAFESTORAGE_PREFIX = "safestorage://";

    public static final String EXTERNAL_LEGAL_FACTS_DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";

    public static final String SAVED_STATUS = "SAVED";


    private final SafeStorageClient safeStorageClient;

    private final HttpConnector httpConnector;


    public String getFileKeyFromUri(String uri) {
        return uri.replace(SAFESTORAGE_PREFIX, "");
    }

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

    public Mono<byte[]> downloadFileInByteArray(String url) {
        return httpConnector.downloadFileInByteArray(url);
    }

    public FileCreationWithContentRequest buildFileCreationWithContentRequest(byte[] bytesPdf) {
        FileCreationWithContentRequest request = new FileCreationWithContentRequest();
        request.setContentType(EXTERNAL_LEGAL_FACTS_DOC_TYPE);
        request.setDocumentType(MediaType.APPLICATION_PDF_VALUE);
        request.setStatus(SAVED_STATUS);
        request.setContent(bytesPdf);

        return request;
    }

    public Mono<String> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
        log.info("Start createAndUploadFile - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

        String sha256 = computeSha256(fileCreationRequest.getContent());

        return safeStorageClient.createFile(fileCreationRequest)
                .onErrorResume(exception ->{
                    log.error("Cannot create file ", exception);
                    return Mono.error(new PnGenericException(ERROR_CODE_PAPERCHANNEL_UPLOADFILEERROR, "Cannot create file "));
                })
                .flatMap(fileCreationResponse -> httpConnector.uploadContent(fileCreationRequest, fileCreationResponse, sha256).thenReturn(fileCreationResponse))
                .map(FileCreationResponseDto::getKey);
    }

    public String computeSha256( byte[] content ) {

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest( content );
            return bytesToBase64( encodedHash );
        } catch (Exception exc) {
            log.error("cannot compute sha256", exc);
            throw new PnGenericException(ERROR_CODE_PAPERCHANNEL_UPLOADFILEERROR, "cannot compute sha256");
        }
    }

    private static String bytesToBase64(byte[] hash) {
        return Base64Utils.encodeToString( hash );
    }


}
