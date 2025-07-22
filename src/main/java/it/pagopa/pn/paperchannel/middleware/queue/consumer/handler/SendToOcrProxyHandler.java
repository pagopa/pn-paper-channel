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
import it.pagopa.pn.paperchannel.middleware.queue.producer.OcrProducer;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

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

    private final OcrProducer ocrProducer;
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

        // Remove suffix 'C' and replace with 'A' or 'B'
        // RECRN00xC -> RECRN00xA
        final String metaStatusCode = changeStatusCodeToMeta(paperRequest.getStatusCode());
        final String dematStatusCode = changeStatusCodeToDemat(paperRequest.getStatusCode());

        var documentType = STATUS_CODE_DOCUMENT_TYPE.get(ExternalChannelCodeEnum.valueOf(dematStatusCode));

        final String metaRequestId = buildMetaRequestId(paperRequest.getRequestId());
        final String dematRequestId = buildDematRequestId(paperRequest.getRequestId());

        // If documentType is null -> statusCode not implemented -> runs only handleMessage and ends
        if (documentType == null || !paperChannelConfig.isEnableOcr()) {
            log.info("StatusCode {} not implemented for OCR, executing only messageHandler", dematStatusCode);
            return messageHandler.handleMessage(entity, paperRequest);
        }

        // Retrieve and save metadata
        var metaEventMono = this.eventMetaDAO
                .getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(metaStatusCode));

        // Retrieve and save demat
        var dematEventMono = this.eventDematDAO
                .getDeliveryEventDemat(dematRequestId, buildDocumentTypeStatusCode(documentType.name(), dematStatusCode));

        // Save meta e demat, then call handler
        return Mono.zip(metaEventMono, dematEventMono)
                .flatMap(tuple -> {
                    PnEventMeta meta = tuple.getT1();
                    PnEventDemat demat = tuple.getT2();

                    return messageHandler.handleMessage(entity, paperRequest)
                            .then(sendOcrIfNeeded(entity, paperRequest, meta, demat, documentType));
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

        if(Boolean.TRUE.equals(entity.getRefined()) && checkDate(meta, demat, paperRequest)) {
            return Mono.zip(safeStorageUrlMono, unifiedDeliveryDriverMono)
                    .flatMap(urlAndDriver -> {
                        var presignedUrl = urlAndDriver.getT1();
                        var unifiedDeliveryDriver = urlAndDriver.getT2();
                        var ocrPayload = buildOcrInputPayload(
                                paperRequest.getRequestId(),
                                documentType,
                                ProductType.valueOf(paperRequest.getProductType()),
                                unifiedDeliveryDriver,
                                paperRequest,
                                presignedUrl);

                        sqsSender.pushToOcr(ocrPayload);
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
     * @param requestId unique request identifier
     * @param documentType type of the document
     * @param productType type of the postal product
     * @param unifiedDeliveryDriver delivery driver ID
     * @param paperRequest original paper request
     * @param attachmentUrl the attachment URL
     * @return the payload to be pushed to the OCR queue
     */
    private static OcrInputPayload buildOcrInputPayload(
            String requestId,
            DocumentType documentType,
            ProductType productType,
            String unifiedDeliveryDriver,
            PaperProgressStatusEventDto paperRequest,
            String attachmentUrl) {
        return OcrInputPayload.builder()
                .commandId(requestId)
                .commandType(OcrInputPayload.CommandType.postal)
                .data(OcrInputPayload.DataDto.builder()
                        .productType(productType)
                        .documentType(documentType)
                        .unifiedDeliveryDriver(UnifiedDeliveryDriver.valueOf(unifiedDeliveryDriver))
                        .details(DetailsDto.builder()
                                .deliveryDetailCode(paperRequest.getStatusCode())
                                .notificationDate(paperRequest.getStatusDateTime().toInstant())
                                .registeredLetterCode(paperRequest.getRegisteredLetterCode())
                                .deliveryFailureCause(DeliveryFailureCause.valueOf(paperRequest.getDeliveryFailureCause()))
                                .attachment(attachmentUrl)
                                .build())
                        .build())
                .build();
    }
}


