package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.PrepareEventMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.F24Service;
import it.pagopa.pn.paperchannel.service.PreparePhaseTwoAsyncService;
import it.pagopa.pn.paperchannel.service.SafeStorageService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.TAKING_CHARGE;
import static it.pagopa.pn.paperchannel.utils.Const.PREFIX_REQUEST_ID_SERVICE_DESK;

@Service
@CustomLog
@RequiredArgsConstructor
public class PreparePhaseTwoAsyncServiceImpl implements PreparePhaseTwoAsyncService {

    private static final String PROCESS_NAME = "Prepare Async Phase Two";

    private final SqsSender sqsSender;
    private final RequestDeliveryDAO requestDeliveryDAO;
    private final F24Service f24Service;
    private final SafeStorageService safeStorageService;
    private final PnPaperChannelConfig paperChannelConfig;
    private final AddressDAO addressDAO;


    @Override
    public Mono<PnDeliveryRequest> prepareAsyncPhaseTwo(PnPrepareDelayerToPaperchannelPayload eventPayload) {
        log.logStartingProcess(PROCESS_NAME);
        Mono<PnDeliveryRequest> deliveryRequestMono = requestDeliveryDAO.getByRequestId(eventPayload.getRequestId(), true);
        return deliveryRequestMono
                .flatMap(deliveryRequest -> {
                    if (f24Service.checkDeliveryRequestAttachmentForF24(deliveryRequest)) {
                        // Calcolo del costo analogico se sono presenti gli F24
                        return f24Service.preparePDF(deliveryRequest);
                    }
                    // Handle regular flow
                    return handleRegularDeliveryRequest(deliveryRequest, eventPayload.getClientId())
                            .onErrorResume(ex -> {
                                // Error -> Retry
                                this.sqsSender.pushErrorDelayerToPaperChannelQueue(eventPayload);
                                return Mono.error(ex);
                            });
                })
                .doOnNext(deliveryRequest -> {
                    log.info("End of prepare async phase two");
                    log.logEndingProcess(PROCESS_NAME);
                })
                .onErrorResume(ex ->  {
                    // F24 Flow error
                    if (ex instanceof PnF24FlowException){
                        return deliveryRequestMono
                                .flatMap(deliveryRequest -> {
                                    manageF24Exception((PnF24FlowException) ex, deliveryRequest);
                                    return Mono.error(ex);
                                });
                    }
                    // Generic error
                    return handlePrepareAsyncPhaseTwoError(eventPayload.getRequestId(), ex);
                });
    }

    /**
     * Handles delivery request processing attachments.
     *
     * @param deliveryRequest the delivery request to process
     * @param clientId
     * @return a Mono containing the processed PnDeliveryRequest
     */
    private Mono<PnDeliveryRequest> handleRegularDeliveryRequest(PnDeliveryRequest deliveryRequest, String clientId) {
        RequestDeliveryMapper.changeState(
                deliveryRequest,
                TAKING_CHARGE.getCode(),
                TAKING_CHARGE.getDescription(),
                TAKING_CHARGE.getDetail(),
                deliveryRequest.getProductType(),
                null
        );
        var correctAddressMono = addressDAO.findByRequestId(deliveryRequest.getRequestId());

        return processAllAttachments(deliveryRequest)
                .flatMap(pnDeliveryRequestWithAttachmentOk -> {
                        correctAddressMono.doOnNext( correctAddress -> {
                            this.pushPrepareEvent(pnDeliveryRequestWithAttachmentOk,
                                    AddressMapper.toDTO(correctAddress), clientId, StatusCodeEnum.OK, null);
                        });
                        return this.requestDeliveryDAO.updateData(pnDeliveryRequestWithAttachmentOk);
                });
    }

    /**
     * Process all attachments of a delivery request.
     *
     * @param deliveryRequest the delivery request to process
     * @return a Mono containing the processed PnDeliveryRequest
     */
    private Mono<PnDeliveryRequest> processAllAttachments(PnDeliveryRequest deliveryRequest) {
        if (areAttachmentsAlreadyProcessed(deliveryRequest)) {
            return Mono.just(deliveryRequest);
        }

        return Flux.fromIterable(deliveryRequest.getAttachments())
                .flatMapSequential(this::processAttachment)
                .doOnNext(attachment -> log.info("Processed attachment={}", attachment))
                .map(AttachmentMapper::toEntity)
                .collectList()
                .map(updatedAttachments ->
                        updateDeliveryRequestWithAttachments(deliveryRequest, updatedAttachments));
    }

    /**
     * Checks if the attachments in the delivery request are already processed.
     *
     * @param deliveryRequest the delivery request to check
     * @return true if the attachments are already processed, false otherwise
     */
    private boolean areAttachmentsAlreadyProcessed(PnDeliveryRequest deliveryRequest) {
        return deliveryRequest.getAttachments().isEmpty() ||
                deliveryRequest.getAttachments().stream()
                        .anyMatch(attachment ->
                                attachment.getNumberOfPage() != null && attachment.getNumberOfPage() > 0);
    }

    /**
     * Processes a single attachment by retrieving its information from Safe Storage.
     * Recupero dei link SafeStorage degli allegati di pagamento di tipo F24
     *
     * @param attachment the attachment to process
     * @return a Mono containing the processed AttachmentInfo
     */
    private Mono<AttachmentInfo> processAttachment(PnAttachmentInfo attachment) {
        return safeStorageService.getFileRecursive(
                        paperChannelConfig.getAttemptSafeStorage(),
                        attachment.getFileKey(),
                        BigDecimal.ZERO)
                // mi serve l'attachment originale
                .map(fileResponse -> Tuples.of(fileResponse, attachment))
                .flatMap(this::mapFileResponseToAttachmentInfo);
    }

    /**
     * Maps a file response and its original attachment to an AttachmentInfo object.
     *
     * @param tuple a tuple containing the file response and the original attachment
     * @return a Mono containing the mapped AttachmentInfo
     */
    private Mono<AttachmentInfo> mapFileResponseToAttachmentInfo(Tuple2<FileDownloadResponseDto, PnAttachmentInfo> tuple) {
        FileDownloadResponseDto fileResponse = tuple.getT1();
        PnAttachmentInfo attachment = tuple.getT2();

        AttachmentInfo attachmentInfo = AttachmentMapper.fromSafeStorage(fileResponse);
        preserveOriginalAttachmentData(attachmentInfo, attachment);

        if (attachmentInfo.getUrl() == null) {
            return Mono.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));
        }

        return safeStorageService.downloadFile(attachmentInfo.getUrl())
                .map(pdDocument -> enrichAttachmentInfoWithDocumentData(attachmentInfo, pdDocument));
    }

    /**
     * Preserves original data from the attachment and sets it in the AttachmentInfo object.
     *
     * @param attachmentInfo the AttachmentInfo to update
     * @param originalAttachment the original attachment containing the data to preserve
     */
    private void preserveOriginalAttachmentData(AttachmentInfo attachmentInfo, PnAttachmentInfo originalAttachment) {
        // Preservo l'eventuale generatedFrom
        attachmentInfo.setGeneratedFrom(originalAttachment.getGeneratedFrom());
        // Preservo l'eventuale docTag
        attachmentInfo.setDocTag(originalAttachment.getDocTag());
        // Preservo l'eventuale resultCode
        attachmentInfo.setFilterResultCode(originalAttachment.getFilterResultCode());
        // Preservo l'eventuale resultDiagnostic
        attachmentInfo.setFilterResultDiagnostic(originalAttachment.getFilterResultDiagnostic());
    }

    /**
     * Enriches the AttachmentInfo object with data from the downloaded PDF document.
     *
     * @param attachmentInfo the AttachmentInfo to enrich
     * @param pdDocument the PDF document containing additional data
     * @return the enriched AttachmentInfo
     */
    private AttachmentInfo enrichAttachmentInfoWithDocumentData(AttachmentInfo attachmentInfo, PDDocument pdDocument) {
        try (pdDocument) {
            Optional.ofNullable(pdDocument.getDocumentInformation())
                    .map(PDDocumentInformation::getCreationDate)
                    .map(Calendar::toInstant)
                    .ifPresent(creationDate -> attachmentInfo.setDate(DateUtils.formatDate(creationDate)));

            attachmentInfo.setNumberOfPage(pdDocument.getNumberOfPages());
        } catch (IOException e) {
            log.error("Error processing PDDocument: {}", e.getMessage(), e);
            throw new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage());
        }
        return attachmentInfo;
    }

    /**
     * Updates the delivery request with the provided list of attachments.
     *
     * @param deliveryRequest the delivery request to update
     * @param updatedAttachments the list of updated attachments
     * @return the updated delivery request
     */
    private PnDeliveryRequest updateDeliveryRequestWithAttachments(PnDeliveryRequest deliveryRequest, List<PnAttachmentInfo> updatedAttachments) {
        deliveryRequest.setAttachments(updatedAttachments);
        return deliveryRequest;
    }

    /**
     * Sends a PrepareEvent based on the given parameters to the appropriate destination (EventBridge or delivery-push).
     *
     * @param request   the delivery request containing the details of the event
     * @param address   the address information to include in the event
     * @param clientId  the client identifier
     * @param statusCode the status code of the event
     * @param koReason  the reason for failure, if applicable
     */
    private void pushPrepareEvent(PnDeliveryRequest request, Address address, String clientId, StatusCodeEnum statusCode, KOReason koReason){
        PrepareEvent prepareEvent = PrepareEventMapper.toPrepareEvent(request, address, statusCode, koReason);
        if (request.getRequestId().contains(PREFIX_REQUEST_ID_SERVICE_DESK)){
            log.info("Sending event to EventBridge: {}", prepareEvent);
            this.sqsSender.pushPrepareEventOnEventBridge(clientId, prepareEvent);
            return;
        }
        log.info("Sending event to delivery-push: {}", prepareEvent);
        this.sqsSender.pushPrepareEvent(prepareEvent);
    }

    /**
     * Handles errors during the Prepare Async Phase Two process by updating the status of the delivery request
     * and logging the error details.
     *
     * @param requestId the identifier of the delivery request
     * @param ex        the exception that occurred
     * @return a Mono that completes with an error after updating the status
     */
    private Mono<PnDeliveryRequest> handlePrepareAsyncPhaseTwoError(String requestId, Throwable ex) {
        log.error("Error prepare async requestId {}, {}", requestId, ex.getMessage(), ex);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        if(ex instanceof PnGenericException pnGenericException) {
            statusDeliveryEnum = exceptionTypeMapper(pnGenericException.getExceptionType());
        }
        return updateStatus(requestId, statusDeliveryEnum)
                .flatMap(entity -> Mono.error(ex));
    }

    private StatusDeliveryEnum exceptionTypeMapper(ExceptionTypeEnum ex){
        return switch (ex) {
            case DOCUMENT_NOT_DOWNLOADED -> StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
            case DOCUMENT_URL_NOT_FOUND -> StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
            default -> StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
        };
    }

    /**
     * Updates the status of a delivery request in db.
     *
     * @param requestId the identifier of the delivery request
     * @param status    the new status to set
     * @return a Mono containing the requestId after the status is updated
     */
    private Mono<String> updateStatus(String requestId, StatusDeliveryEnum status ){
        String processName = "Update Status";
        log.logStartingProcess(processName);

        var statusCode = status.getCode();
        var completeDescription = new StringBuilder(status.getCode());
        var statusDate = DateUtils.formatDate(Instant.now());
        var statusDetail = status.getDetail();

        if (StringUtils.isNotBlank(status.getDescription())) {
            completeDescription.append(" - ").append(status.getDescription());
        }

        return this.requestDeliveryDAO.updateStatus(requestId, statusCode, completeDescription.toString(), statusDetail, statusDate).thenReturn(requestId)
                .doOnSuccess(requestIdString -> log.logEndingProcess(processName));
    }

    private void manageF24Exception(PnF24FlowException ex, PnDeliveryRequest deliveryRequest) {

        F24Error f24Error = ex.getF24Error();
        log.error(ex.getMessage(), ex);

        int attempt = f24Error.getAttempt();
        f24Error.setAttempt(attempt +1);

        changeStatusDeliveryRequest(deliveryRequest, StatusDeliveryEnum.F24_ERROR);
        log.info("attempting to pushing to internal payload={}", ex);
        sqsSender.pushInternalError(f24Error, f24Error.getAttempt(), F24Error.class);
    }

    private Mono<PnDeliveryRequest> changeStatusDeliveryRequest(PnDeliveryRequest deliveryRequest, StatusDeliveryEnum status){
        RequestDeliveryMapper.changeState(
                deliveryRequest,
                status.getCode(),
                status.getDescription(),
                status.getDetail(),
                deliveryRequest.getProductType(), null);
        return this.requestDeliveryDAO.updateData(deliveryRequest).flatMap(Mono::just);
    }
}
