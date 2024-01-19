package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.MetadataPagesDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.F24Client;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.F24Error;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.F24Service;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.*;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.F24_WAITING;

@CustomLog
@Service
public class F24ServiceImpl extends GenericService implements F24Service {

    public static final String URL_PROTOCOL_F24 = "f24set";
    private static final String SAFESTORAGE_PREFIX = "safestorage://";
    private static final String REWORK_COUNT_SUFFIX_REQUEST_ID = ".REWORK_";

    private final F24Client f24Client;
    private final PaperCalculatorUtils paperCalculatorUtils;
    private final AddressDAO addressDAO;
    private final PnPaperChannelConfig paperChannelConfig;

    private final SafeStorageClient safeStorageClient;

    private final HttpConnector httpConnector;

    private final DateChargeCalculationModesUtils dateChargeCalculationModesUtils;


    public F24ServiceImpl(PnAuditLogBuilder auditLogBuilder, F24Client f24Client,
                          SqsSender sqsQueueSender,
                          PaperCalculatorUtils paperCalculatorUtils, AddressDAO addressDAO, RequestDeliveryDAO requestDeliveryDAO,
                          PnPaperChannelConfig paperChannelConfig, SafeStorageClient safeStorageClient, HttpConnector httpConnector,
                          DateChargeCalculationModesUtils dateChargeCalculationModesUtils) {
        super(auditLogBuilder, sqsQueueSender, requestDeliveryDAO);
        this.f24Client = f24Client;
        this.paperCalculatorUtils = paperCalculatorUtils;
        this.addressDAO = addressDAO;
        this.requestDeliveryDAO = requestDeliveryDAO;
        this.paperChannelConfig = paperChannelConfig;
        this.safeStorageClient = safeStorageClient;
        this.httpConnector = httpConnector;
        this.dateChargeCalculationModesUtils = dateChargeCalculationModesUtils;
    }

    @Override
    public boolean checkDeliveryRequestAttachmentForF24(PnDeliveryRequest deliveryRequest) {
        Optional<PnAttachmentInfo> optF24Attachment = getF24PnAttachmentInfo(deliveryRequest);
        return optF24Attachment.isPresent();
    }

    @NotNull
    private Optional<PnAttachmentInfo> getF24PnAttachmentInfo(PnDeliveryRequest deliveryRequest) {
        return deliveryRequest.getAttachments().stream().filter(x -> x.getFileKey().startsWith(URL_PROTOCOL_F24)).findFirst();
    }

    @Override
    public Mono<PnDeliveryRequest> preparePDF(PnDeliveryRequest deliveryRequest) {

        return getF24PnAttachmentInfo(deliveryRequest)
                .map(pnAttachmentInfo -> {
                    pnAttachmentInfo.setDocumentType(Const.DOCUMENT_TYPE_F24_SET);
                    return preparePdfAndEnrichDeliveryRequest(deliveryRequest, pnAttachmentInfo);
                })
                .orElseGet(() -> Mono.error(new PnInternalException("missing URL f24set on f24serviceImpl", PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR)));
    }

    @Override
    public Mono<PnDeliveryRequest> arrangeF24AttachmentsAndReschedulePrepare(String requestIdFromF24, List<String> generatedUrls) {
        //recupero la requestId dell'entità eliminando eventualmente il .REWORK_n
        String requestIdDeliveryRequest = requestIdFromF24.replaceAll("\\.REWORK_\\d", "");
        // sistemo gli allegati sostituendoli all'originale, salvo e faccio ripartire l'evento di prepare
        final List<String> normalizedFilekeys = normalizeGeneratedUrls(generatedUrls);
        return requestDeliveryDAO.getByRequestId(requestIdDeliveryRequest)
                        .map(pnDeliveryRequest -> arrangeAttachments(pnDeliveryRequest, normalizedFilekeys))
                        .flatMap(requestDeliveryDAO::updateData)
                        .flatMap(deliveryRequest -> {
                            PrepareAsyncRequest request = new PrepareAsyncRequest(deliveryRequest.getRequestId(), deliveryRequest.getIun(), false, 0);
                            request.setF24ResponseFlow(true);
                            this.sqsSender.pushToInternalQueue(request);
                            return Mono.just(deliveryRequest);
                        })
                .doOnSuccess(deliveryRequest -> f24ResponseLogAuditSuccess(deliveryRequest, normalizedFilekeys))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage())))
                .doOnError(deliveryRequest -> f24ResponseLogAuditFailure(requestIdFromF24, normalizedFilekeys));

    }

    private Mono<PnDeliveryRequest> preparePdfAndEnrichDeliveryRequest(PnDeliveryRequest deliveryRequest, PnAttachmentInfo f24Attachment) {

        String requestIdForF24Prepare = Boolean.TRUE.equals(deliveryRequest.getReworkNeeded()) ? deliveryRequest.getRequestId() + REWORK_COUNT_SUFFIX_REQUEST_ID + deliveryRequest.getReworkNeededCount() : deliveryRequest.getRequestId();
        return this.parseF24URL(f24Attachment.getFileKey())
                .flatMap(f24AttachmentInfo -> enrichWithAnalogCostIfNeeded(f24AttachmentInfo, deliveryRequest, f24Attachment))
                /* Call preparePDF request API and propagate Analog Cost */
                .doOnSuccess(f24AttachmentInfo -> logAuditBefore("preparePDF requestId = %s, relatedRequestId = %s engaging F24 ", deliveryRequest))
                .flatMap(f24AttachmentInfo -> f24Client.preparePDF(requestIdForF24Prepare, f24AttachmentInfo.getSetId(), f24AttachmentInfo.getRecipientIndex(), sumCostAndAnalogCost(f24AttachmentInfo)).thenReturn(f24AttachmentInfo))
                .doOnNext(f24AttachmentInfo -> logAuditSuccess("preparePDF requestId = %s, relatedRequestId = %s successfully sent to F24", deliveryRequest))
                /* Insert Analog Cost and change status of delivery request to F24_WAITING */
                .map(f24AttachmentInfo -> this.enrichDeliveryRequest(deliveryRequest, F24_WAITING, f24AttachmentInfo.getAnalogCost(), null))
                .map(this::restoreNumberOfPagesAndDocTypeAtNullIfCOMPLETEMode)
                .map(this::resetReworkNeededFlag)
                /* Update delivery request on database */
                .flatMap(this.requestDeliveryDAO::updateData)
                .onErrorResume(ex -> catchThrowableAndThrowPnF24FlowException(ex, deliveryRequest.getIun(), deliveryRequest.getRequestId(), deliveryRequest.getRelatedRequestId()));
    }

    private Mono<F24AttachmentInfo> enrichWithAnalogCostIfNeeded(F24AttachmentInfo f24AttachmentInfo, PnDeliveryRequest deliveryRequest, PnAttachmentInfo f24Attachment) {
        if (f24AttachmentInfo.getCost() != null && f24AttachmentInfo.getCost() > 0) {

            if(dateChargeCalculationModesUtils.getChargeCalculationMode() == ChargeCalculationModeEnum.COMPLETE) {
                log.debug("enrichWithAnalogCost for COMPLETE");
                return handlerF24ForCOMPLETEMode( f24AttachmentInfo, deliveryRequest, f24Attachment)
                        .flatMap(numberOfPagesResponseDto -> enrichWithAnalogCost(deliveryRequest, f24AttachmentInfo));
            }
            else {
                log.debug("enrichWithAnalogCost for AAR");
                return enrichWithAnalogCost(deliveryRequest, f24AttachmentInfo);
            }
        } else {
            log.debug("Skipped enrichWithAnalogCostIfNeeded because cost is: {}", f24AttachmentInfo.getCost());
            return Mono.just(f24AttachmentInfo);
        }
    }

    private Mono<F24AttachmentInfo> handlerF24ForCOMPLETEMode(F24AttachmentInfo f24AttachmentInfo, PnDeliveryRequest deliveryRequest, PnAttachmentInfo f24Attachment) {
        logAuditBefore("getNumberOfPages requestId = %s, relatedRequestId = %s engaging F24 ", deliveryRequest);
        /* Retrieve F24 number of pages */
        return f24Client.getNumberOfPages(f24AttachmentInfo.getSetId(), f24AttachmentInfo.getRecipientIndex())
                .doOnNext(numberOfPagesResponseDto -> logAuditSuccess("getNumberOfPages requestId = %s, relatedRequestId = %s successfully sent to F24", deliveryRequest))
                .map(numberOfPagesResponseDto -> {
                    /* Enrich original F24 attachment number of pages field */
                    log.debug("F24Client.getNumberOfPages response: {}", numberOfPagesResponseDto);
                    List<MetadataPagesDto> metadataPagesDtoList = numberOfPagesResponseDto.getF24Set() != null ? numberOfPagesResponseDto.getF24Set() : Collections.emptyList();

                    f24AttachmentInfo.setNumberOfPage(getNumberOfPagesFromF24Attachment(metadataPagesDtoList));
                    f24Attachment.setNumberOfPage(getNumberOfPagesFromF24Attachment(metadataPagesDtoList));

                    return numberOfPagesResponseDto;
                })
                .flatMap(numberOfPagesResponseDto -> enrichAttachmentsNotF24WithPageNumber(deliveryRequest).thenReturn(numberOfPagesResponseDto))
                .doOnNext(numberOfPagesResponseDto -> {
                    if(log.isDebugEnabled()) {
                        List<Tuple2<String, Integer>> tuples = deliveryRequest.getAttachments().stream()
                                .map(pnAttachmentInfo -> Tuples.of(pnAttachmentInfo.getFileKey(), pnAttachmentInfo.getNumberOfPage()))
                                .toList();
                        log.debug("Attachments with number of pages after enrichment: {}", tuples);
                    }
                })
                .thenReturn(f24AttachmentInfo);
    }

    // Se la modalità è COMPLETE, ho "valorizzato" il numberOfPage di tutti gli attachments e valorizzato il documentType dell'ARR
    // con questo metodo ripristino tutto a come era in origine. Gestione che va rifattorizzata
    private PnDeliveryRequest restoreNumberOfPagesAndDocTypeAtNullIfCOMPLETEMode(PnDeliveryRequest pnDeliveryRequest) {
        if(dateChargeCalculationModesUtils.getChargeCalculationMode() == ChargeCalculationModeEnum.COMPLETE) {
            pnDeliveryRequest.getAttachments()
                    .forEach(pnAttachmentInfo -> {
                        pnAttachmentInfo.setNumberOfPage(null);
                        if(!pnAttachmentInfo.getFileKey().startsWith(URL_PROTOCOL_F24)) {
                            pnAttachmentInfo.setDocumentType(null);
                        }
                    });
        }
        return pnDeliveryRequest;
    }

    private PnDeliveryRequest resetReworkNeededFlag(PnDeliveryRequest pnDeliveryRequest) {
        pnDeliveryRequest.setReworkNeeded(null);
        return pnDeliveryRequest;
    }

    private Integer getNumberOfPagesFromF24Attachment(List<MetadataPagesDto> metadataPagesList) {

        return metadataPagesList.stream()
                .filter(metadataPagesDto -> metadataPagesDto != null && metadataPagesDto.getNumberOfPages() != null)
                .map(metadataPagesDto -> metadataPagesDto.getNumberOfPages() % 2 == 0 ? metadataPagesDto.getNumberOfPages() : metadataPagesDto.getNumberOfPages() + 1)
                .reduce(0, Integer::sum);
    }

    // In fase di PREPARE nella modalità COMPLETE, siccome ho necessità di conoscere le pagine anche degli attachments non f24,
    // chiamo safe-storage. Anche nella fase di SEND chiamo safe-storage per il ricalcolo.
    private Mono<PnDeliveryRequest> enrichAttachmentsNotF24WithPageNumber(PnDeliveryRequest deliveryRequest) {

        log.debug("BEFORE filter getNumberOfPagesWithoutF24 Attachments: {}", deliveryRequest.getAttachments());
        List<PnAttachmentInfo> pnAttachmentInfosWithoutF24 = deliveryRequest.getAttachments().stream()
                .filter(pnAttachmentInfo -> !pnAttachmentInfo.getFileKey().startsWith(URL_PROTOCOL_F24))
                .toList();

        log.debug("AFTER filter getNumberOfPagesWithoutF24 Attachments: {}", pnAttachmentInfosWithoutF24);
        return Flux.fromIterable(pnAttachmentInfosWithoutF24)
                .parallel()
                .flatMap(attachment -> getFileRecursive(
                        paperChannelConfig.getAttemptSafeStorage(),
                        attachment.getFileKey(),
                        new BigDecimal(0))
                        .map(r -> Tuples.of(r, attachment)) // mi serve l'attachment originale
                )
                .flatMap(fileResponseAndOrigAttachment -> {
                    if (fileResponseAndOrigAttachment.getT1().getDownload() == null || fileResponseAndOrigAttachment.getT1().getDownload().getUrl() == null)
                        return Flux.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));
                    return httpConnector.downloadFile(fileResponseAndOrigAttachment.getT1().getDownload().getUrl())
                            .map(pdDocument -> {
                                try {
                                    log.debug("enrichAttachmentsNotF24WithPageNumber Key: {}, totalPages: {}", fileResponseAndOrigAttachment.getT1().getKey(), pdDocument.getNumberOfPages());
                                    fileResponseAndOrigAttachment.getT2().setNumberOfPage(pdDocument.getNumberOfPages());
                                    fileResponseAndOrigAttachment.getT2().setDocumentType(fileResponseAndOrigAttachment.getT1().getDocumentType());
                                    pdDocument.close();
                                } catch (IOException e) {
                                    throw new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage());
                                }
                                return deliveryRequest;
                            });

                })
                .sequential()
                .collectList()
                .thenReturn(deliveryRequest);
    }

    public Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis){
        if (n<0)
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

    private PnDeliveryRequest enrichDeliveryRequest(PnDeliveryRequest deliveryRequest, StatusDeliveryEnum status, Integer analogCost, Instant statusDate) {
        RequestDeliveryMapper.changeState(
                deliveryRequest,
                status.getCode(),
                status.getDescription(),
                status.getDetail(),
                deliveryRequest.getProductType(),
                statusDate
        );

        deliveryRequest.setCost(analogCost);
        return deliveryRequest;
    }

    private PnDeliveryRequest arrangeAttachments(PnDeliveryRequest pnDeliveryRequest, List<String> generatedUrls){
        List<PnAttachmentInfo> attachments = new ArrayList<>();
        pnDeliveryRequest.getAttachments().forEach(pnAttachmentInfo -> {
            if (pnAttachmentInfo.getDocumentType() != null && pnAttachmentInfo.getDocumentType().equals(Const.DOCUMENT_TYPE_F24_SET))
            {
                // nel caso l'attachement fosse di tipo DOCUMENT_TYPE_F24_SET, vado a sovrascriverlo con la lista tornata da f24.
                generatedUrls.forEach(url -> {
                    PnAttachmentInfo newAttachment = new PnAttachmentInfo();
                    newAttachment.setUrl(null);
                    newAttachment.setFileKey(url);
                    newAttachment.setGeneratedFrom(pnAttachmentInfo.getFileKey()); // url originale f24
                    attachments.add(newAttachment);
                });
            }
            else
                attachments.add(pnAttachmentInfo);

        });
        pnDeliveryRequest.setAttachments(attachments);
        return pnDeliveryRequest;
    }

    private List<String> normalizeGeneratedUrls(List<String> generatedUrls){
        return generatedUrls.stream().map(x -> x.startsWith(SAFESTORAGE_PREFIX)?x:(SAFESTORAGE_PREFIX +x)).toList();
    }

    private Integer sumCostAndAnalogCost(F24AttachmentInfo f24AttachmentInfo) {
        return f24AttachmentInfo.getCost()==null? null: f24AttachmentInfo.getCost() + f24AttachmentInfo.getAnalogCost();
    }

    private Mono<F24AttachmentInfo> enrichWithAnalogCost(PnDeliveryRequest deliveryRequest, F24AttachmentInfo pnAttachmentInfo) {
        logAuditBefore("preparePDF requestId = %s, relatedRequestId = %s prepare F24", deliveryRequest);


        return addressDAO.findByRequestId(deliveryRequest.getRequestId())
                .switchIfEmpty(Mono.defer(() -> {
                    logAuditSuccess("preparePDF requestId = %s, relatedRequestId = %s Receiver address is not present on DB!!", deliveryRequest);
                    log.error("Receiver Address for {} request id not found on DB", deliveryRequest.getRequestId());
                    throw new PnInternalException(ADDRESS_NOT_EXIST.name(), ADDRESS_NOT_EXIST.getMessage());
                }))
                .map(AddressMapper::toDTO)
                .flatMap(receiverAddress -> paperCalculatorUtils.calculator(deliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList(),
                        receiverAddress,
                        ProductTypeEnum.fromValue(deliveryRequest.getProductType()),
                        StringUtils.equals(deliveryRequest.getPrintType(), Const.BN_FRONTE_RETRO)))
                .map(analogCost -> {
                    pnAttachmentInfo.setAnalogCost(Utility.toCentsFormat(analogCost));
                    return pnAttachmentInfo;
                })
                .doOnNext(analogCost -> logAuditSuccess("preparePDF requestId = %s, relatedRequestId = %s Receiver address is present on DB, computed cost " + analogCost, deliveryRequest));
    }

    private Mono<F24AttachmentInfo> parseF24URL(String f24url) {
        try {
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(f24url).build();
            MultiValueMap<String, String> parameters = uriComponents.getQueryParams();
            // il costo 0 o nullo equivale a "non mi interessa il calcolo del costo"
            // per semplicità metto a null
            Integer cost = parameters.containsKey("cost")?Integer.parseInt(parameters.get("cost").get(0)):null;
            if (cost != null && cost == 0)
                cost = null;

            return Mono.just(F24AttachmentInfo.builder()
                    .setId(uriComponents.getHost())
                    .recipientIndex(uriComponents.getPathSegments().get(0))
                    .cost(cost)
                    .analogCost(null)
                    .build());

        } catch (Exception e) {
            log.error("cannot parse f24url={}", f24url, e);
            return Mono.error(e);
        }
    }


    @Builder
    @Getter
    @Setter
    private static class F24AttachmentInfo{
        private Integer cost;
        private Integer analogCost;
        private Integer numberOfPage;
        private String setId;
        private String recipientIndex;
    }


    private <T> Mono<T> catchThrowableAndThrowPnF24FlowException(Throwable ex, String iun, String requestId, String correlationId) {
        F24Error error = new F24Error();
        error.setIun(iun);
        error.setMessage(ex.getMessage());
        error.setRequestId(requestId);
        error.setRelatedRequestId(correlationId);
        error.setAttempt(0);

        PnF24FlowException pnF24FlowException = new PnF24FlowException(ExceptionTypeEnum.F24_ERROR, error, ex);
        return Mono.error(pnF24FlowException);
    }

    private void logAuditSuccess(String message, PnDeliveryRequest deliveryRequest){
        pnLogAudit.addsSuccessResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }

    private void logAuditBefore(String message, PnDeliveryRequest deliveryRequest){
        pnLogAudit.addsBeforeResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }


    private void f24ResponseLogAuditSuccess(PnDeliveryRequest entity, List<String> generatedUrls) {

            String docs = String.join(",", generatedUrls);

            pnLogAudit.addsSuccessResolveLogic(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, traceId = %s generated f24 docs = %s Response OK from F24 service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            entity.getCorrelationId(),
                            docs));
    }

    private void f24ResponseLogAuditFailure(String requestId, List<String> generatedUrls) {

            String docs = String.join(",", generatedUrls==null?List.of("null"):generatedUrls);

            pnLogAudit.addsFailLog(
                    PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC,
                    requestId,
                    String.format("prepare requestId = %s generated f24 docs = %s error from F24 service",
                            requestId, docs));

    }


}
