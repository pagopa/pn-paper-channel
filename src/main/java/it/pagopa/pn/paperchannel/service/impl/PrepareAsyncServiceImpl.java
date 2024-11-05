package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.PrepareEventMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.math.BigDecimal;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.TAKING_CHARGE;
import static it.pagopa.pn.paperchannel.utils.Const.PREFIX_REQUEST_ID_SERVICE_DESK;

@Service
@CustomLog
public class PrepareAsyncServiceImpl extends BaseService implements PaperAsyncService {

    private final SafeStorageService safeStorageService;
    private final AddressDAO addressDAO;
    private final PnPaperChannelConfig paperChannelConfig;
    private final PaperRequestErrorDAO paperRequestErrorDAO;
    private final PaperAddressService paperAddressService;
    private final PaperCalculatorUtils paperCalculatorUtils;
    private final F24Service f24Service;
    private final AttachmentsConfigService attachmentsConfigService;

    public PrepareAsyncServiceImpl(NationalRegistryClient nationalRegistryClient,
                                   RequestDeliveryDAO requestDeliveryDAO, SqsSender sqsQueueSender, CostDAO costDAO,
                                   SafeStorageService safeStorageService, AddressDAO addressDAO, PnPaperChannelConfig paperChannelConfig,
                                   PaperRequestErrorDAO paperRequestErrorDAO, PaperAddressService paperAddressService,
                                   PaperCalculatorUtils paperCalculatorUtils, F24Service f24Service, AttachmentsConfigService attachmentsConfigService) {
        super(requestDeliveryDAO, costDAO, nationalRegistryClient, sqsQueueSender);
        this.safeStorageService = safeStorageService;
        this.addressDAO = addressDAO;
        this.paperChannelConfig = paperChannelConfig;
        this.paperRequestErrorDAO = paperRequestErrorDAO;
        this.paperAddressService = paperAddressService;
        this.paperCalculatorUtils = paperCalculatorUtils;
        this.f24Service = f24Service;
        this.attachmentsConfigService = attachmentsConfigService;
    }

    @Override
    public Mono<PnDeliveryRequest> prepareAsync(PrepareAsyncRequest request){
        final String processName = "Prepare Async";
        log.logStartingProcess(processName);

        String correlationId = request.getCorrelationId();
        final String requestId = request.getRequestId();
        Address addressFromNationalRegistry = request.getAddress();

        Mono<PnDeliveryRequest> requestDeliveryEntityMono;
        if (correlationId!= null) {
            log.info("Start async for {} correlation id", request.getCorrelationId());
            requestDeliveryEntityMono = requestDeliveryDAO.getByCorrelationId(correlationId, true);
        } else {
            log.info("Start async for {} request id", request.getRequestId());
            requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId, true);
        }
        return requestDeliveryEntityMono
                .flatMap(deliveryRequest -> {
                    if (request.isF24ResponseFlow()) {
                        log.info("just returning find address because is on F24 response flow and there is no need to check address again");
                        return this.addressDAO.findByRequestId(deliveryRequest.getRequestId()).zipWhen(pnAddress -> Mono.just(deliveryRequest));
                    }
                    else {
                        return checkAndUpdateAddress(deliveryRequest, addressFromNationalRegistry, request)
                                .zipWhen(pnAddress -> attachmentsConfigService.filterAttachmentsToSend(deliveryRequest, deliveryRequest.getAttachments(), pnAddress));
                    }
                }) // nel caso sia settato il flag di f24FlowResponse, vuol dire che ho già eseguito questo step.
                .flatMap(pnDeliveryRequestWithAddress -> {
                    var pnDeliveryRequest = pnDeliveryRequestWithAddress.getT2();
                    var correctAddress = pnDeliveryRequestWithAddress.getT1();
                    if (f24Service.checkDeliveryRequestAttachmentForF24(pnDeliveryRequest)) {
                        return f24Service.preparePDF(pnDeliveryRequest);
                    }
                    else {
                        return continueWithPrepareRequest(pnDeliveryRequest, request, correctAddress);
                    }
                })
                .onErrorResume(ex -> handlePrepareAsyncError(request, processName, correlationId, requestId, ex));
    }

    @NotNull
    private Mono<PnDeliveryRequest> handlePrepareAsyncError(PrepareAsyncRequest request, String processName, String correlationId, String requestId, Throwable ex) {
        log.error("Error prepare async requestId {}, {}", requestId, ex.getMessage(), ex);
        if (ex instanceof PnAddressFlowException
            || ex instanceof PnF24FlowException) return Mono.error(ex);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
        if(ex instanceof PnGenericException pnGenericException) {
            statusDeliveryEnum = mapper(pnGenericException.getExceptionType());
        }
        return updateStatus(requestId, correlationId, statusDeliveryEnum)
                .doOnNext(entity -> {
                    if (entity.getStatusCode().equals(StatusDeliveryEnum.UNTRACEABLE.getCode())) {
                        sendUnreachableEvent(entity, request.getClientId(), getKOReason(ex));
                        log.logEndingProcess(processName);
                    }
                })
                .flatMap(entity -> Mono.error(ex));
    }

    private Mono<PnDeliveryRequest> continueWithPrepareRequest(PnDeliveryRequest pnDeliveryRequest, PrepareAsyncRequest request, PnAddress correctAddress){
        RequestDeliveryMapper.changeState(
                pnDeliveryRequest,
                TAKING_CHARGE.getCode(),
                TAKING_CHARGE.getDescription(),
                TAKING_CHARGE.getDetail(),
                pnDeliveryRequest.getProductType(),
                null
        );

        return getAttachmentsInfo(pnDeliveryRequest, request)
                .flatMap(pnDeliveryRequestWithAttachmentOk -> {
                            this.pushPrepareEvent(pnDeliveryRequestWithAttachmentOk, AddressMapper.toDTO(correctAddress), request.getClientId(), StatusCodeEnum.OK, null);
                            return this.requestDeliveryDAO.updateData(pnDeliveryRequestWithAttachmentOk);
                        }
                );
    }

    private KOReason getKOReason(Throwable ex) {
        if(ex instanceof PnUntracebleException untEx) {
            return untEx.getKoReason();
        }
        else {
            return null;
        }
    }

    private StatusDeliveryEnum mapper(ExceptionTypeEnum ex){
        return switch (ex) {
            case UNTRACEABLE_ADDRESS -> StatusDeliveryEnum.UNTRACEABLE;
            case DOCUMENT_NOT_DOWNLOADED -> StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
            case DOCUMENT_URL_NOT_FOUND -> StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
            default -> StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
        };

    }

    private Mono<PnDeliveryRequest> updateStatus(String requestId, String correlationId, StatusDeliveryEnum status ){
        String processName = "Update Status";
        log.logStartingProcess(processName);
        Mono<PnDeliveryRequest> pnDeliveryRequest;
        if (StringUtils.isNotEmpty(requestId) && !StringUtils.isNotEmpty(correlationId) ){
            pnDeliveryRequest= this.requestDeliveryDAO.getByRequestId(requestId);
        }else{
            pnDeliveryRequest= this.requestDeliveryDAO.getByCorrelationId(correlationId);
        }
        log.logEndingProcess(processName);
        return pnDeliveryRequest.flatMap(
                entity -> {
                    RequestDeliveryMapper.changeState(
                            entity,
                            status.getCode(),
                            status.getDescription(),
                            status.getDetail(),
                            null,
                            null
                    );
                    return this.requestDeliveryDAO.updateData(entity);
                });
    }

    private Mono<PnAddress> checkAndUpdateAddress(PnDeliveryRequest pnDeliveryRequest, Address fromNationalRegistries, PrepareAsyncRequest queueModel){
        final String VALIDATION_NAME = "Check and update address";
        return this.paperAddressService.getCorrectAddress(pnDeliveryRequest, fromNationalRegistries, queueModel)
                .flatMap(newAddress -> {
                    log.logCheckingOutcome(VALIDATION_NAME, true);
                    pnDeliveryRequest.setAddressHash(newAddress.convertToHash());
                    pnDeliveryRequest.setProductType(this.paperCalculatorUtils.getProposalProductType(newAddress, pnDeliveryRequest.getProposalProductType()));

                    //set flowType per TTL
                    newAddress.setFlowType(Const.PREPARE);
                    return addressDAO.create(AddressMapper.toEntity(newAddress, pnDeliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, paperChannelConfig));
                })
                .onErrorResume(PnGenericException.class, ex -> handleAndThrowAgainError(ex, pnDeliveryRequest.getRequestId()));
    }

    private Mono<PnAddress> handleAndThrowAgainError(PnGenericException ex, String requestId) {
        if(ex instanceof PnUntracebleException) {
            // se l'eccezione PnGenericException è di tipo UNTRACEABLE, ALLORA NON SCRIVO L'ERRORE SU DB
            return Mono.error(ex);
        } else {
            // ALTRIMENTI SCRIVO L'ERRORE SU DB
            return traceError(requestId, ex, "CHECK_ADDRESS_FLOW").then(Mono.error(ex));
        }
    }


    private Mono<PnDeliveryRequest> getAttachmentsInfo(PnDeliveryRequest deliveryRequest, PrepareAsyncRequest request){

        if(deliveryRequest.getAttachments().isEmpty() ||
                !deliveryRequest.getAttachments().stream().filter(a ->a.getNumberOfPage()!=null && a.getNumberOfPage()>0).toList().isEmpty()){
            return Mono.just(deliveryRequest);
        }

        return Flux.fromStream(deliveryRequest.getAttachments().stream())
                .flatMapSequential( attachment -> safeStorageService.getFileRecursive(
                        paperChannelConfig.getAttemptSafeStorage(),
                        attachment.getFileKey(),
                        new BigDecimal(0))
                        .map(r -> Tuples.of(r, attachment)) // mi serve l'attachment originale
                        .flatMap(fileResponseAndOrigAttachment -> {
                            AttachmentInfo info = AttachmentMapper.fromSafeStorage(fileResponseAndOrigAttachment.getT1());
                            info.setGeneratedFrom(fileResponseAndOrigAttachment.getT2().getGeneratedFrom()); // preservo l'eventuale generatedFrom
                            info.setDocTag(fileResponseAndOrigAttachment.getT2().getDocTag()); // preservo l'eventuale docTag
                            info.setFilterResultCode(fileResponseAndOrigAttachment.getT2().getFilterResultCode()); // preservo
                            info.setFilterResultDiagnostic(fileResponseAndOrigAttachment.getT2().getFilterResultDiagnostic()); // preservo
                            if (info.getUrl() == null)
                                return Mono.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));
                            return safeStorageService.downloadFile(info.getUrl())
                                    .map(pdDocument -> {
                                        try {
                                            if (pdDocument.getDocumentInformation() != null && pdDocument.getDocumentInformation().getCreationDate() != null) {
                                                info.setDate(DateUtils.formatDate(pdDocument.getDocumentInformation().getCreationDate().toInstant()));
                                            }
                                            info.setNumberOfPage(pdDocument.getNumberOfPages());
                                            pdDocument.close();
                                        } catch (IOException e) {
                                            throw new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage());
                                        }
                                        return info;
                                    });

                        })
                )
                .doOnNext(x -> log.info("processed attachment={}", x))
                .map(AttachmentMapper::toEntity)
                .collectList()
                .map(listAttachment -> {
                    deliveryRequest.setAttachments(listAttachment);
                    return deliveryRequest;
                })
                .onErrorResume(ex -> {
                    request.setIun(deliveryRequest.getIun());
                    this.sqsSender.pushInternalError(request, request.getAttemptRetry(), PrepareAsyncRequest.class);
                    return Mono.error(ex);
                });
    }



    private void sendUnreachableEvent(PnDeliveryRequest request, String clientId, KOReason koReason){
        log.debug("Send Unreachable Event request id - {}, iun - {}", request.getRequestId(), request.getIun());
        this.pushPrepareEvent(request, null, clientId, StatusCodeEnum.KO, koReason);
    }

    private Mono<Void> traceError(String requestId, PnGenericException ex, String flowType){
        String geokey = (ex instanceof PnDeduplicationException) ? ((PnDeduplicationException) ex).getGeokey() : null;

        PnRequestError pnRequestError = PnRequestError.builder()
                .requestId(requestId)
                .error(ex.getMessage())
                .flowThrow(flowType)
                .geokey(geokey)
                .build();

        return this.paperRequestErrorDAO.created(pnRequestError).then();
    }

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

}
