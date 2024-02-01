package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AttachmentDetails;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.model.DematInternalEvent;
import it.pagopa.pn.paperchannel.service.DematZipService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.SafeStorageUtils;
import it.pagopa.pn.paperchannel.utils.ZipUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;
import static it.pagopa.pn.paperchannel.utils.SafeStorageUtils.*;

@Service
public class DematZipServiceImpl extends GenericService implements DematZipService {

    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final SafeStorageUtils safeStorageUtils;



    public DematZipServiceImpl(PnAuditLogBuilder auditLogBuilder, SqsSender sqsSender, RequestDeliveryDAO requestDeliveryDAO,
                               PnPaperChannelConfig pnPaperChannelConfig, SafeStorageUtils safeStorageUtils) {
        super(auditLogBuilder, sqsSender, requestDeliveryDAO);
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.safeStorageUtils = safeStorageUtils;
    }

    @Override
    public Mono<Void> handle(DematInternalEvent dematInternalEvent) {
        String fileKeyZip = safeStorageUtils.getFileKeyFromUri(dematInternalEvent.getAttachmentDetails().getUrl());
        return safeStorageUtils.getFileRecursive(pnPaperChannelConfig.getAttemptSafeStorage(), fileKeyZip, BigDecimal.ZERO)
                .flatMap(this::checkValidUrl)
                .flatMap(fileDownloadResponseDto -> safeStorageUtils.downloadFileInByteArray(fileDownloadResponseDto.getDownload().getUrl()))
                .map(ZipUtils::extractPdfFromZip)
                .map(safeStorageUtils::buildFileCreationWithContentRequest)
                .flatMap(safeStorageUtils::createAndUploadContent)
                .map(fileKeyPdf -> buildSendEventsForZipAndPdf(dematInternalEvent, fileKeyPdf))
                .doOnNext(sendEvents -> sendEvents.forEach(sqsSender::pushSendEvent))
                .then();
    }

    private Mono<FileDownloadResponseDto> checkValidUrl(FileDownloadResponseDto fileDownloadResponseDto) {
        if (fileDownloadResponseDto.getDownload() == null || fileDownloadResponseDto.getDownload().getUrl() == null) {
            return Mono.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));
        }
        return Mono.just(fileDownloadResponseDto);
    }

    @NotNull
    private List<SendEvent> buildSendEventsForZipAndPdf(DematInternalEvent dematInternalEvent, String fileKeyPdf) {
        SendEvent sendEventZipMessage = SendEventMapper.createSendEventMessage(dematInternalEvent);
        SendEvent sendEventPdfMessage = SendEventMapper.createSendEventMessage(dematInternalEvent);
        AttachmentDetails attachmentDetails = sendEventPdfMessage.getAttachments().get(0);
        var attachmentDetailsPDF = new AttachmentDetails()
                .id(attachmentDetails.getId() + 1)
                .documentType(MediaType.APPLICATION_PDF_VALUE)
                .date(Instant.now())
                .url(enrichWithPrefixIfNeeded(fileKeyPdf));

        sendEventPdfMessage.setAttachments(List.of(attachmentDetailsPDF));
        return List.of(sendEventZipMessage, sendEventPdfMessage);
    }

    private String enrichWithPrefixIfNeeded(String url) {
        if(! url.startsWith(SAFESTORAGE_PREFIX)) url = SAFESTORAGE_PREFIX + url;
        return url;
    }
}
