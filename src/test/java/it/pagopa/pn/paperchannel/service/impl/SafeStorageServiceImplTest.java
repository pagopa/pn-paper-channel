package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafeStorageServiceImplTest {

    @Mock
    private SafeStorageClient safeStorageClient;

    @Mock
    private HttpConnector httpConnector;

    @InjectMocks
    private SafeStorageServiceImpl safeStorageService;

    @Test
    void getFileRecursiveOk() {
        String fileKey = "fileKeyPdf";
        int attempt = 0;
        FileDownloadResponseDto response = new FileDownloadResponseDto().key(fileKey);
        when(safeStorageClient.getFile(fileKey)).thenReturn(Mono.just(response));
        StepVerifier.create(safeStorageService.getFileRecursive(attempt, fileKey, BigDecimal.ZERO))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void getFileRecursiveWithNegativeN() {
        String fileKey = "fileKeyPdf";
        int attempt = -1;
        StepVerifier.create(safeStorageService.getFileRecursive(attempt, fileKey, BigDecimal.ZERO))
                .expectError(PnGenericException.class)
                .verify();
    }

    @Test
    void getFileRecursiveOkWithFinishedAttempts() {
        String fileKey = "fileKeyPdf";
        int attempt = 3;
        when(safeStorageClient.getFile(fileKey)).thenReturn(Mono.error(new PnRetryStorageException(BigDecimal.ZERO)));
        StepVerifier.create(safeStorageService.getFileRecursive(attempt, fileKey, BigDecimal.ZERO))
                .expectError(PnGenericException.class)
                .verify();

        verify(safeStorageClient, times(3 + 1)).getFile(fileKey);
    }

    @Test
    void downloadFileAsByteArrayOk() {
        String url = "http://[bucket_name].s3.amazonaws.com/";
        byte[] response = "A VALUE".getBytes();
        when(httpConnector.downloadFileAsByteArray(url)).thenReturn(Mono.just(response));
        StepVerifier.create(safeStorageService.downloadFileAsByteArray(url))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void downloadFileOk() {
        String url = "http://[bucket_name].s3.amazonaws.com/";
        var response = readPdf(getFile("zip/test.pdf"));
        when(httpConnector.downloadFile(url)).thenReturn(Mono.just(response));
        StepVerifier.create(safeStorageService.downloadFile(url))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void createAndUploadContentOk() {
        FileCreationWithContentRequest request = new FileCreationWithContentRequest();
        request.setContent("A VALUE".getBytes());
        request.setContentType(MediaType.APPLICATION_PDF_VALUE);
        request.setStatus("SAVED");
        request.setDocumentType("EXTERNAL_LEGAL_FACT");

        String sha256 = safeStorageService.computeSha256(request.getContent());


        FileCreationResponse responseFromCreateFile = new FileCreationResponse();
        responseFromCreateFile.setKey("fileKey");
        responseFromCreateFile.setSecret("secret");
        when(safeStorageClient.createFile(request, sha256)).thenReturn(Mono.just(responseFromCreateFile));
        when(httpConnector.uploadContent(request, responseFromCreateFile, sha256)).thenReturn(Mono.empty());

        StepVerifier.create(safeStorageService.createAndUploadContent(request))
                .expectNext(responseFromCreateFile.getKey())
                .verifyComplete();
    }

    @Test
    void createAndUploadContentKo() {
        FileCreationWithContentRequest request = new FileCreationWithContentRequest();
        request.setContent("A VALUE".getBytes());
        request.setContentType(MediaType.APPLICATION_PDF_VALUE);
        request.setStatus("SAVED");
        request.setDocumentType("EXTERNAL_LEGAL_FACT");

        String sha256 = safeStorageService.computeSha256(request.getContent());

        when(safeStorageClient.createFile(request, sha256)).thenReturn(Mono.error(WebClientResponseException.create(502, "", new HttpHeaders(), null, null)));

        StepVerifier.create(safeStorageService.createAndUploadContent(request))
                .expectError(PnGenericException.class)
                .verify();
    }

    private byte[] getFile(String file) {
        try {
            return new ClassPathResource(file).getInputStream().readAllBytes();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PDDocument readPdf(byte[] pdfFiles) {
        try(PDDocument pdf = PDDocument.load(pdfFiles)) {
            return pdf;
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
