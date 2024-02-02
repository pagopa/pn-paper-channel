package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AttachmentDetails;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.model.DematInternalEvent;
import it.pagopa.pn.paperchannel.service.DematZipService;
import it.pagopa.pn.paperchannel.service.SafeStorageService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;
import static it.pagopa.pn.paperchannel.service.impl.SafeStorageServiceImpl.*;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildDematRequestId;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildDocumentTypeStatusCode;

@Service
@Slf4j
public class DematZipServiceImpl extends GenericService implements DematZipService {

    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final SafeStorageService safeStorageService;

    private final EventDematDAO eventDematDAO;



    public DematZipServiceImpl(PnAuditLogBuilder auditLogBuilder, SqsSender sqsSender, RequestDeliveryDAO requestDeliveryDAO,
                               PnPaperChannelConfig pnPaperChannelConfig, SafeStorageService safeStorageService, EventDematDAO eventDematDAO) {
        super(auditLogBuilder, sqsSender, requestDeliveryDAO);
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.safeStorageService = safeStorageService;
        this.eventDematDAO = eventDematDAO;
    }

    @Override
    public Mono<Void> handle(DematInternalEvent dematInternalEvent) {
        String fileKeyZip = safeStorageService.getFileKeyFromUri(dematInternalEvent.getAttachmentDetails().getUrl());
        log.info("FileKey zip: {}", fileKeyZip);
        return safeStorageService.getFileRecursive(pnPaperChannelConfig.getAttemptSafeStorage(), fileKeyZip, BigDecimal.ZERO)
                .doOnNext(fileDownloadResponseDto -> log.debug("Response from getFileRecursive: {}", fileDownloadResponseDto))
                .flatMap(this::checkValidUrl)
                .flatMap(fileDownloadResponseDto -> safeStorageService.downloadFileInByteArray(fileDownloadResponseDto.getDownload().getUrl()))
                .doOnNext(bytes -> log.debug("Download file done"))
                .map(ZipUtils::extractPdfFromZip)
                .doOnNext(bytes -> log.debug("Extract PDF from ZIP done"))
                .map(safeStorageService::buildFileCreationWithContentRequest)
                .flatMap(safeStorageService::createAndUploadContent)
                .doOnNext(bytes -> log.debug("Upload PDF to S3 done"))
                .flatMap(fileKeyPdf -> saveDematAndSendEvents(dematInternalEvent, fileKeyPdf));
    }

    private Mono<FileDownloadResponseDto> checkValidUrl(FileDownloadResponseDto fileDownloadResponseDto) {
        if (fileDownloadResponseDto.getDownload() == null || fileDownloadResponseDto.getDownload().getUrl() == null) {
            return Mono.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));
        }
        return Mono.just(fileDownloadResponseDto);
    }

    @NotNull
    private SendEvent buildSendEventsForZipAndPdf(DematInternalEvent dematInternalEvent, AttachmentDetails attachmentDetails) {
        SendEvent sendEventMessage = SendEventMapper.createSendEventMessage(dematInternalEvent);

        sendEventMessage.setAttachments(List.of(attachmentDetails));
        return sendEventMessage;
    }

    private String enrichWithPrefixIfNeeded(String url) {
        if(! url.startsWith(SAFESTORAGE_PREFIX)) url = SAFESTORAGE_PREFIX + url;
        return url;
    }

    private Mono<Void> saveDematAndSendEvents(DematInternalEvent dematInternalEvent, String fileKeyPdf) {
        var attachmentDetailsPDF = new AttachmentDetails()
                .id(dematInternalEvent.getAttachmentDetails().getId() + 1)
                .documentType(MediaType.APPLICATION_PDF_VALUE)
                .date(Instant.now())
                .url(enrichWithPrefixIfNeeded(fileKeyPdf));

        var pnEventDematZip = buildPnEventDemat(dematInternalEvent, dematInternalEvent.getAttachmentDetails());
        var pnEventDematPdf = buildPnEventDemat(dematInternalEvent, attachmentDetailsPDF);
        var sendEventZip = buildSendEventsForZipAndPdf(dematInternalEvent, dematInternalEvent.getAttachmentDetails());
        var sendEventPdf = buildSendEventsForZipAndPdf(dematInternalEvent, attachmentDetailsPDF);
        var sendEvents = List.of(sendEventZip, sendEventPdf);

        return Flux.fromIterable(List.of(pnEventDematZip, pnEventDematPdf))
                .flatMap(eventDematDAO::createOrUpdate)
                .doOnNext(pnEventDemat -> log.debug("Save demat: {}", pnEventDemat))
                .collectList()
                .doOnNext(pnEventDematsSaved -> sendEvents.forEach(sqsSender::pushSendEvent))
                .doOnNext(pnEventDematsSaved -> log.info("Sent events: {}", sendEvents))
                .then();

    }

    protected PnEventDemat buildPnEventDemat(DematInternalEvent dematInternalEvent, AttachmentDetails attachmentDetails) {
        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDematRequestId(buildDematRequestId(dematInternalEvent.getRequestId()));
        pnEventDemat.setDocumentTypeStatusCode(buildDocumentTypeStatusCode(attachmentDetails.getDocumentType(), dematInternalEvent.getStatusCode()));
        pnEventDemat.setTtl(dematInternalEvent.getStatusDateTime().plusDays(pnPaperChannelConfig.getTtlExecutionDaysDemat()).toEpochSecond());

        pnEventDemat.setRequestId(dematInternalEvent.getRequestId());
        pnEventDemat.setStatusCode(dematInternalEvent.getStatusCode());
        pnEventDemat.setDocumentType(attachmentDetails.getDocumentType());
        pnEventDemat.setDocumentDate(attachmentDetails.getDate());
        pnEventDemat.setStatusDateTime(dematInternalEvent.getStatusDateTime().toInstant());
        pnEventDemat.setUri(attachmentDetails.getUrl());
        return pnEventDemat;
    }
}
