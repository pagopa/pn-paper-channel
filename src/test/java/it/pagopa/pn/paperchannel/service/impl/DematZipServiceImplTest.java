package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadInfoDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AttachmentDetails;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.DematInternalEvent;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.SafeStorageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.OffsetDateTime;

import static it.pagopa.pn.paperchannel.utils.SafeStorageUtils.SAFESTORAGE_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DematZipServiceImplTest {


    private DematZipServiceImpl dematZipService;

    private SqsSender sqsSender;

    private RequestDeliveryDAO requestDeliveryDAO;

    private PnPaperChannelConfig pnPaperChannelConfig;

    private SafeStorageClient safeStorageClient;

    private SafeStorageUtils safeStorageUtils;

    private HttpConnector httpConnector;

    @BeforeEach
    public void init() {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        sqsSender = mock(SqsSender.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);
        safeStorageClient = mock(SafeStorageClient.class);
        httpConnector = mock(HttpConnector.class);
        pnPaperChannelConfig = mock(PnPaperChannelConfig.class);
        safeStorageUtils = new SafeStorageUtils(safeStorageClient, httpConnector);
        when(pnPaperChannelConfig.getAttemptSafeStorage()).thenReturn(1);
        dematZipService = new DematZipServiceImpl(auditLogBuilder, sqsSender, requestDeliveryDAO, pnPaperChannelConfig, safeStorageUtils);
    }

    @Test
    void handleOk() {
        String fileKey = "fileKeyZip.zip";
        String s3Url = "http://[bucket_name].s3.amazonaws.com/";
        DematInternalEvent dematInternalEvent = DematInternalEvent.builder()
                .requestId("requestId")
                .statusDateTime(OffsetDateTime.now())
                .statusDetail(StatusCodeEnum.PROGRESS.getValue())
                .attemptRetry(0)
                .attachmentDetails(new AttachmentDetails().url(SAFESTORAGE_PREFIX + fileKey))
                .build();

        FileDownloadResponseDto zipSafeStorageResponse = new FileDownloadResponseDto();
        zipSafeStorageResponse.setDownload(new FileDownloadInfoDto().url(s3Url));
        when(safeStorageClient.getFile(fileKey)).thenReturn(Mono.just(zipSafeStorageResponse));

        when(httpConnector.downloadFileInByteArray(s3Url)).thenReturn(Mono.just(getFile("zip/zip-with-pdf-and-xml.zip")));


        FileCreationWithContentRequest fileCreationRequestWithContent = safeStorageUtils.buildFileCreationWithContentRequest(getFile("zip/test.pdf"));
        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto().secret("secret").key("fileKeyPdf.pdf");
        String sha256 = safeStorageUtils.computeSha256(fileCreationRequestWithContent.getContent());
        when(safeStorageClient.createFile(fileCreationRequestWithContent))
                .thenReturn(Mono.just(fileCreationResponseDto));

        when(httpConnector.uploadContent(fileCreationRequestWithContent, fileCreationResponseDto, sha256)).thenReturn(Mono.empty());

        StepVerifier.create(dematZipService.handle(dematInternalEvent))
                .verifyComplete();

        verify(sqsSender, times(2)).pushSendEvent(any());
    }

    @Test
    void handleKoBecauseSafeStorageKo() {
        String fileKey = "fileKeyZip.zip";
        String s3Url = "http://[bucket_name].s3.amazonaws.com/";
        DematInternalEvent dematInternalEvent = DematInternalEvent.builder()
                .requestId("requestId")
                .statusDateTime(OffsetDateTime.now())
                .statusDetail(StatusCodeEnum.PROGRESS.getValue())
                .attemptRetry(0)
                .attachmentDetails(new AttachmentDetails().url(SAFESTORAGE_PREFIX + fileKey))
                .build();

        FileDownloadResponseDto zipSafeStorageResponse = new FileDownloadResponseDto();
        zipSafeStorageResponse.setDownload(new FileDownloadInfoDto().url(s3Url));
        when(safeStorageClient.getFile(fileKey)).thenReturn(Mono.error(WebClientResponseException.create(502,  "", new HttpHeaders(), null, null)));

        when(httpConnector.downloadFileInByteArray(s3Url)).thenReturn(Mono.just(getFile("zip/zip-with-pdf-and-xml.zip")));


        FileCreationWithContentRequest fileCreationRequestWithContent = safeStorageUtils.buildFileCreationWithContentRequest(getFile("zip/test.pdf"));
        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto().secret("secret").key("fileKeyPdf.pdf");
        String sha256 = safeStorageUtils.computeSha256(fileCreationRequestWithContent.getContent());
        when(safeStorageClient.createFile(fileCreationRequestWithContent))
                .thenReturn(Mono.just(fileCreationResponseDto));

        when(httpConnector.uploadContent(fileCreationRequestWithContent, fileCreationResponseDto, sha256)).thenReturn(Mono.empty());

        StepVerifier.create(dematZipService.handle(dematInternalEvent))
                .verifyComplete();

        verify(sqsSender, times(2)).pushSendEvent(any());
    }

    private byte[] getFile(String file) {
        try {
            return new ClassPathResource(file).getInputStream().readAllBytes();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
