package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.MessageDigest;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ERROR_CODE_PAPERCHANNEL_ZIP_HANDLE;

public interface SafeStorageService {

    String SAFESTORAGE_PREFIX = "safestorage://";
    String ZIP_HANDLE_DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS_REPLICA";
    String SAVED_STATUS = "SAVED";


    default FileCreationWithContentRequest buildFileCreationWithContentRequest(byte[] bytesPdf) {
        FileCreationWithContentRequest request = new FileCreationWithContentRequest();
        request.setContentType(MediaType.APPLICATION_PDF_VALUE);
        request.setDocumentType(ZIP_HANDLE_DOC_TYPE);
        request.setStatus(SAVED_STATUS);
        request.setContent(bytesPdf);

        return request;
    }

    default String computeSha256( byte[] content ) {

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest( content );
            return bytesToBase64( encodedHash );
        } catch (Exception exc) {
            throw new PnGenericException(ERROR_CODE_PAPERCHANNEL_ZIP_HANDLE, exc.getMessage());
        }
    }

    private String bytesToBase64(byte[] hash) {
        return Base64Utils.encodeToString( hash );
    }

    Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis);
    Mono<byte[]> downloadFileAsByteArray(String url);
    Mono<PDDocument> downloadFile(String url);
    Mono<String> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);
}
