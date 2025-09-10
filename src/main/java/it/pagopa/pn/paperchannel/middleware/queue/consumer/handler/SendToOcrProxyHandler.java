package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.OcrInputPayload;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;
import static it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum.*;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildDocumentTypeStatusCode;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaRequestId;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaStatusCode;
import static it.pagopa.pn.paperchannel.middleware.queue.model.OcrInputPayload.DataDto.*;
import static it.pagopa.pn.paperchannel.middleware.queue.model.OcrInputPayload.DataDto.DetailsDto.*;

/**
 * Handler that acts as a middleware between the start and the end of the final handlers.
 * It checks meta and demat events, and if successful, it triggers the sending of related
 * information to the OCR system after sending it to pn-delivery-push.
 */
@Slf4j
@SuperBuilder
public class SendToOcrProxyHandler implements MessageHandler {
    private static final Map<ExternalChannelCodeEnum, DocumentType> STATUS_CODE_DOCUMENT_TYPE = Map.of(
            RECRN001B, DocumentType.AR,
            RECRN002B, DocumentType.Plico,
            RECRN002E, DocumentType.Plico,
            RECRN003B, DocumentType.AR,
            RECRN004B, DocumentType.Plico,
            RECRN005B, DocumentType.Plico
    );

    private final EventDematDAO eventDematDAO;
    private final EventMetaDAO eventMetaDAO;
    private final MessageHandler messageHandler;
    private final SafeStorageClient safeStorageClient;
    private final PaperChannelDeliveryDriverDAO deliveryDriverDAO;
    private final PnPaperChannelConfig paperChannelConfig;
    private final SqsSender sqsSender;

    /**
     * Retrieves metadata and dematerialized document data associated with the paper request.
     * If the timestamps are consistent and the status code matches a known document type,
     * it sends the information to the OCR system after executing the wrapped handler logic.
     *
     * @param entity the delivery request entity
     * @param paperRequest the event containing delivery progress information
     * @return a reactive Mono that completes when the message is fully handled
     */
    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.info("{} handling statusCode={}", SendToOcrProxyHandler.class.getSimpleName(), paperRequest.getStatusCode());

        // Remove suffix 'C' and replace with 'A' or 'B'
        // RECRN00xC -> RECRN00xA
        Optional<String> metaStatusCode = parseMetaStatusCode(paperRequest.getStatusCode());
        Optional<String> dematStatusCode = parseDematStatusCode(paperRequest.getStatusCode());
        Optional<DocumentType> documentType = dematStatusCode.flatMap(this::getDocumentType);

        // If meta, demat or documentType is empty -> statusCode not implemented -> runs only handleMessage and ends
        if (metaStatusCode.isEmpty() || dematStatusCode.isEmpty() ||
                documentType.isEmpty()) {
            log.warn("StatusCode {} not implemented for OCR, executing only messageHandler", paperRequest.getStatusCode());
            return messageHandler.handleMessage(entity, paperRequest);
        }
        if (!paperChannelConfig.isEnableOcr()) {
            log.info("EnableOcr config false, executing only messageHandler");
            return messageHandler.handleMessage(entity, paperRequest);
        }

        final String metaRequestId = buildMetaRequestId(paperRequest.getRequestId());
        final String dematRequestId = buildDematRequestId(paperRequest.getRequestId());

        // Retrieve and save metadata
        var metaEventMono = this.eventMetaDAO
                .getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(metaStatusCode.get()))
                .map(Optional::of)
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.info("EventMeta not found for requestId: {}", metaRequestId);
                    return Optional.empty();
                }));

        // Retrieve and save demat
        var dematEventMono = this.eventDematDAO
                .getDeliveryEventDemat(dematRequestId, buildDocumentTypeStatusCode(documentType.get().name(),
                        dematStatusCode.get()))
                .map(Optional::of)
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.info("EventDemat not found for requestId: {}", dematRequestId);
                    return Optional.empty();
                }));

        // Save meta e demat, then call handler
        return Mono.zip(metaEventMono, dematEventMono)
                .flatMap(tuple -> {
                    Optional<PnEventMeta> metaOpt = tuple.getT1();
                    Optional<PnEventDemat> dematOpt = tuple.getT2();

                    return messageHandler.handleMessage(entity, paperRequest)
                            .then(Mono.defer(() -> {
                                if (metaOpt.isPresent() && dematOpt.isPresent()) {
                                    return sendOcrIfNeeded(
                                            entity,
                                            paperRequest,
                                            metaOpt.get(),
                                            dematOpt.get(),
                                            documentType.get());
                                } else {
                                    log.info("Skipping OCR: meta or demat is missing.");
                                    return Mono.empty();
                                }
                            }));
                });
    }

    /**
     * Sends data to OCR only if the delivery is refined and all timestamps match.
     *
     * @param entity the delivery request
     * @param paperRequest the paper event
     * @param meta metadata retrieved from DB
     * @param demat dematerialized document retrieved from DB
     * @param documentType the determined document type
     * @return a Mono indicating the completion of the operation
     */
    private Mono<Void> sendOcrIfNeeded(
            PnDeliveryRequest entity,
            PaperProgressStatusEventDto paperRequest,
            PnEventMeta meta,
            PnEventDemat demat,
            DocumentType documentType) {

        var safeStorageUrlMono = getSafeStoragePresignedUrl(demat.getUri());
        var unifiedDeliveryDriverMono = getUnifiedDeliveryDriver(entity.getDriverCode());

        if(Boolean.TRUE.equals(entity.getRefined()) &&
                checkDate(meta, demat, paperRequest) &&
                isPdfDocument(demat.getUri())){
            return Mono.zip(safeStorageUrlMono, unifiedDeliveryDriverMono)
                    .flatMap(urlAndDriver -> {
                        var presignedUrl = urlAndDriver.getT1();
                        var unifiedDeliveryDriver = urlAndDriver.getT2();
                        var ocrPayload = buildOcrInputPayload(
                                documentType,
                                ProductType.valueOf(paperRequest.getProductType()),
                                unifiedDeliveryDriver,
                                paperRequest,
                                presignedUrl);

                        sqsSender.pushToOcr(ocrPayload);
                        return Mono.empty();
                    })
                    .onErrorResume((e) -> {
                        log.error("Error on sending payload to OCR:", e);
                        return Mono.empty();
                    })
                    .then();
        }
        return Mono.empty();
    }

    /**
     * Verifies that all event timestamps match.
     *
     * @param meta the metadata event
     * @param demat the dematerialized document event
     * @param paperRequest the paper event containing the final timestamp
     * @return true if all dates match; false otherwise
     */
    private static boolean checkDate(PnEventMeta meta, PnEventDemat demat, PaperProgressStatusEventDto paperRequest) {
        Instant metaDate = meta.getStatusDateTime();
        Instant dematDate = demat.getStatusDateTime();
        Instant finalDate = paperRequest.getStatusDateTime().toInstant();

        return metaDate.equals(dematDate) && metaDate.equals(finalDate);
    }

    /**
     * Retrieves the unified delivery driver string by delivery driver ID.
     *
     * @param deliveryDriverId the delivery driver ID
     * @return Mono containing the unified driver string
     */
    private Mono<String> getUnifiedDeliveryDriver(String deliveryDriverId) {
        return deliveryDriverDAO.getByDeliveryDriverId(deliveryDriverId)
                .map(PaperChannelDeliveryDriver::getUnifiedDeliveryDriver);
    }

    /**
     * Retrieves the presigned download URL from the SafeStorage.
     *
     * @param safeStorageUri the object key
     * @return Mono containing the presigned URL
     */
    private Mono<String> getSafeStoragePresignedUrl(String safeStorageUri) {
        return safeStorageClient.getFile(safeStorageUri).flatMap(fileResponse -> {
            if (fileResponse.getDownload() == null || fileResponse.getDownload().getUrl() == null) {
                return Mono.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));
            }
            return Mono.just(fileResponse.getDownload().getUrl());
        });
    }

    /**
     * Builds the payload to be sent to the OCR service.
     *
     * @param documentType type of the document
     * @param productType type of the postal product
     * @param unifiedDeliveryDriver delivery driver ID
     * @param paperRequest original paper request
     * @param attachmentUrl the attachment URL
     * @return the payload to be pushed to the OCR queue
     */
    private static OcrInputPayload buildOcrInputPayload(
            DocumentType documentType,
            ProductType productType,
            String unifiedDeliveryDriver,
            PaperProgressStatusEventDto paperRequest,
            String attachmentUrl) {
        return OcrInputPayload.builder()
                .commandId(paperRequest.getRequestId() + "#" + UUID.randomUUID())
                .commandType(OcrInputPayload.CommandType.postal)
                .data(OcrInputPayload.DataDto.builder()
                        .productType(productType)
                        .documentType(documentType)
                        .unifiedDeliveryDriver(UnifiedDeliveryDriver.valueOf(unifiedDeliveryDriver))
                        .details(DetailsDto.builder()
                                .deliveryDetailCode(paperRequest.getStatusCode())
                                .notificationDate(paperRequest.getStatusDateTime().toInstant())
                                .registeredLetterCode(paperRequest.getRegisteredLetterCode())
                                .deliveryFailureCause(
                                        Optional.ofNullable(paperRequest.getDeliveryFailureCause())
                                                .map(DeliveryFailureCause::valueOf)
                                                .orElse(null)
                                )
                                .attachment(attachmentUrl)
                                .build())
                        .build())
                .build();
    }

    /**
     * Parses a status code and attempts to convert it to its META equivalent.
     * If the conversion fails due to an invalid input, returns an empty Optional.
     *
     * @param statusCode the original status code to convert
     * @return an Optional containing the Meta-equivalent status code, or empty if conversion fails
     */
    private Optional<String> parseMetaStatusCode(String statusCode) {
        try {
            return Optional.of(changeStatusCodeToMeta(statusCode));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a status code and attempts to convert it to its DEMAT equivalent.
     * If the conversion fails due to an invalid input, returns an empty Optional.
     *
     * @param statusCode the original status code to convert
     * @return an Optional containing the Demat-equivalent status code, or empty if conversion fails
     */
    private Optional<String> parseDematStatusCode(String statusCode) {
        try {
            return Optional.of(changeStatusCodeToDemat(statusCode));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Attempts to retrieve the {@link DocumentType} associated with the given Demat status code.
     * If the status code is not valid or not mapped, returns an empty Optional.
     *
     * @param dematStatusCode the Demat status code
     * @return an Optional containing the corresponding DocumentType, or empty if not found or invalid
     */
    private Optional<DocumentType> getDocumentType(String dematStatusCode) {
        try {
            return Optional.ofNullable(STATUS_CODE_DOCUMENT_TYPE.get(ExternalChannelCodeEnum.valueOf(dematStatusCode)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if a SafeStorage uri is a PDF or not
     * @param safeStorageUri file uri
     * @return true if file is a PDF, false otherwise
     */
    private boolean isPdfDocument(String safeStorageUri){
        return safeStorageUri.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }
}


