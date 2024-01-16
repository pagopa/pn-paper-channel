package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;

@Component
@CustomLog
@AllArgsConstructor
public class AttachmentUtils {

    private final SafeStorageClient safeStorageClient;
    private final PnPaperChannelConfig paperChannelConfig;

    public Mono<PnDeliveryRequest> enrichAttachmentInfos(PnDeliveryRequest deliveryRequest, boolean excludeF24) {

        List<PnAttachmentInfo> attachments = deliveryRequest.getAttachments();

        Stream<PnAttachmentInfo> attachmentStream = excludeF24
                ? attachments.stream().filter(pnAttachmentInfo -> !pnAttachmentInfo.getFileKey().startsWith(Const.URL_PROTOCOL_F24))
                : attachments.stream();

        return Flux.fromStream(attachmentStream)
                .parallel()
                .filter(pnAttachmentInfo -> pnAttachmentInfo.getNumberOfPage() != null && pnAttachmentInfo.getNumberOfPage() > 0)
                .flatMap(attachment -> getFileRecursive(
                        paperChannelConfig.getAttemptSafeStorage(),
                        attachment.getFileKey(),
                        new BigDecimal(0))
                        .map(r -> Tuples.of(r, attachment))
                )
                .flatMap(fileResponseAndOrigAttachment -> {

                    FileDownloadResponseDto fileDownloadResponseDto = fileResponseAndOrigAttachment.getT1();
                    PnAttachmentInfo attachmentInfo = fileResponseAndOrigAttachment.getT2();

                    attachmentInfo.setFileKey(fileDownloadResponseDto.getKey());
                    attachmentInfo.setChecksum(fileDownloadResponseDto.getChecksum());
                    attachmentInfo.setDocumentType(fileDownloadResponseDto.getDocumentType());

                    if (fileDownloadResponseDto.getDownload() != null && fileDownloadResponseDto.getDownload().getUrl() != null) {
                        attachmentInfo.setUrl(fileDownloadResponseDto.getDownload().getUrl());
                    }

                    if (attachmentInfo.getUrl() == null)
                        return Mono.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));

                    return this.downloadAndEnrichAttachment(attachmentInfo);

                })
                .sequential()
                .collectList()
                .map(listAttachment -> deliveryRequest);
    }

    public Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis){

        if (n < 0) return Mono.error(new PnGenericException( DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage() ) );

        return Mono.delay(Duration.ofMillis(millis.longValue()))
                .flatMap(item -> safeStorageClient.getFile(fileKey))
                .map(fileDownloadResponseDto -> fileDownloadResponseDto)
                .onErrorResume(ex -> {
                    log.error ("Error with retrieve {}", ex.getMessage());
                    return Mono.error(ex);
                })
                .onErrorResume(PnRetryStorageException.class, ex -> getFileRecursive(n - 1, fileKey, ex.getRetryAfter()));
    }

    public Mono<PnAttachmentInfo> downloadAndEnrichAttachment(PnAttachmentInfo attachmentInfo) {

        return HttpConnector.downloadFile(attachmentInfo.getUrl())
                .map(pdDocument -> {
                    try {
                        if (pdDocument.getDocumentInformation() != null && pdDocument.getDocumentInformation().getCreationDate() != null) {
                            attachmentInfo.setDate(DateUtils.formatDate(pdDocument.getDocumentInformation().getCreationDate().toInstant()));
                        }
                        attachmentInfo.setNumberOfPage(pdDocument.getNumberOfPages());
                        pdDocument.close();
                    } catch (IOException e) {
                        throw new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage());
                    }
                    return attachmentInfo;
                }).onErrorResume(Mono::error);
    }
}
