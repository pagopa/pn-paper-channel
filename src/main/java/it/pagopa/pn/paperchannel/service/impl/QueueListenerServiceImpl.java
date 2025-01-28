package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEvent;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEventItem;
import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.FeedbackStatus;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;

import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.commons.log.PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.READY_TO_SEND;
import static it.pagopa.pn.paperchannel.utils.Utility.resolveAuditLogFromResponse;

@Service
@CustomLog
public class QueueListenerServiceImpl extends GenericService implements QueueListenerService {
    private static final String NATIONAL_REGISTRY_DESCRIPTION = "Retrieve the address.";
    private static final String PROCESS_NAME = "National Registries Response Listener";

    private final PaperResultAsyncService paperResultAsyncService;
    private final PaperAsyncService paperAsyncService;
    private final PreparePhaseOneAsyncService preparePhaseOneAsyncService;
    private final PreparePhaseTwoAsyncService preparePhaseTwoAsyncService;
    private final AddressDAO addressDAO;
    private final PaperRequestErrorDAO paperRequestErrorDAO;
    private final ExternalChannelClient externalChannelClient;
    private final F24Service f24Service;
    private final DematZipService dematZipService;
    private final AttachmentsConfigService attachmentsConfigService;
    private final PrepareFlowStarter prepareFlowStarter;
    private final NationalRegistryService nationalRegistryService;

    public QueueListenerServiceImpl(RequestDeliveryDAO requestDeliveryDAO,
                                    SqsSender sqsSender,
                                    PaperResultAsyncService paperResultAsyncService,
                                    PaperAsyncService paperAsyncService,
                                    PreparePhaseOneAsyncService preparePhaseOneAsyncService,
                                    PreparePhaseTwoAsyncService preparePhaseTwoAsyncService,
                                    AddressDAO addressDAO,
                                    PaperRequestErrorDAO paperRequestErrorDAO,
                                    ExternalChannelClient externalChannelClient,
                                    F24Service f24Service,
                                    DematZipService dematZipService,
                                    AttachmentsConfigService attachmentsConfigService,
                                    PrepareFlowStarter prepareFlowStarter, NationalRegistryService nationalRegistryService) {

        super(sqsSender, requestDeliveryDAO);

        this.paperResultAsyncService = paperResultAsyncService;
        this.paperAsyncService = paperAsyncService;
        this.preparePhaseOneAsyncService = preparePhaseOneAsyncService;
        this.preparePhaseTwoAsyncService = preparePhaseTwoAsyncService;
        this.addressDAO = addressDAO;
        this.paperRequestErrorDAO = paperRequestErrorDAO;
        this.externalChannelClient = externalChannelClient;
        this.f24Service = f24Service;
        this.dematZipService = dematZipService;
        this.attachmentsConfigService = attachmentsConfigService;
        this.prepareFlowStarter = prepareFlowStarter;
        this.nationalRegistryService = nationalRegistryService;
    }


    /**
     * @deprecated This method has been replaced by  {@link #normalizeAddressListener(PrepareNormalizeAddressEvent, int)}.
     */
    @Deprecated(since = "2.15.0", forRemoval = true)
    @Override
    public void internalListener(PrepareAsyncRequest body, int attempt) {
        String processName = "InternalListener";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, body.getRequestId());
        log.logStartingProcess(processName);
        MDCUtils.addMDCToContextAndExecute(Mono.just(body)
                        .flatMap(prepareRequest -> {
                            prepareRequest.setAttemptRetry(attempt);
                            return this.paperAsyncService.prepareAsync(prepareRequest);
                        })
                        .doOnSuccess(resultFromAsync ->{
                                    log.info("End of prepare async internal");
                                    log.logEndingProcess(processName);
                                }
                        )
                        .doOnError(throwable -> {
                            log.error(throwable.getMessage());
                            if (throwable instanceof PnAddressFlowException) return;
                            if (throwable instanceof PnF24FlowException pnF24FlowException) manageF24Exception(pnF24FlowException.getF24Error(), pnF24FlowException.getF24Error().getAttempt(), pnF24FlowException);

                            throw new PnGenericException(PREPARE_ASYNC_LISTENER_EXCEPTION, PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage());
                        }))
                .block();
    }

    @Override
    public void normalizeAddressListener(PrepareNormalizeAddressEvent data, int attempt) {
        String processName = "NormalizeAddressListener";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, data.getRequestId());
        log.logStartingProcess(processName);
        MDCUtils.addMDCToContextAndExecute(Mono.just(data)
                        .flatMap(prepareNormalizeAddressEvent -> {
                            prepareNormalizeAddressEvent.setAttempt(attempt);
                            return this.preparePhaseOneAsyncService.preparePhaseOneAsync(prepareNormalizeAddressEvent);
                        })
                        .doOnSuccess(resultFromAsync -> log.logEndingProcess(processName)
                        )
                )
                .block();
    }

    @Override
    public void dematZipInternalListener(DematInternalEvent body, int attempt) {
        String processName = "DematZipInternalListener";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, body.getRequestId());
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, processName);
        log.logStartingProcess(processName);
        MDCUtils.addMDCToContextAndExecute(Mono.just(body)
                        .flatMap(dematInternalEvent -> {
                            dematInternalEvent.setAttemptRetry(attempt);
                            return this.dematZipService.handle(dematInternalEvent);
                        })
                        .doOnSuccess(resultFromAsync ->{
                                    log.info("End of dematZipInternalListener");
                                    log.logEndingProcess(processName);
                                }
                        )
                        .doOnError(throwable -> {
                            log.error("Error in dematZipInternalListener", throwable);
                            body.setErrorMessage(throwable.getMessage());
                            this.sqsSender.pushInternalError(body, body.getAttemptRetry(), DematInternalEvent.class);
                        }))
                .block();
    }


    public void f24ErrorListener(F24Error entity, Integer attempt) {
        log.info("Start async for {} request id", entity.getRequestId());

        String processName = "F24 retry listener logic";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, entity.getRequestId());
        log.logStartingProcess(processName);
        MDCUtils.addMDCToContextAndExecute(requestDeliveryDAO.getByRequestId(entity.getRequestId(), true)
                        .flatMap(f24Service::preparePDF)
                        .doOnSuccess(pnRequestError -> log.logEndingProcess(processName))
                        .doOnError(throwable ->  log.logEndingProcess(processName, false, throwable.getMessage()))
                        .onErrorResume(ex -> {
                            manageF24Exception(entity, attempt, ex);
                            return Mono.error(new PnF24FlowException(F24_ERROR, entity, ex));
                        })
                        .then())
                .block();
    }

    private void manageF24Exception(F24Error entity, Integer attempt, Throwable ex) {
        log.error(ex.getMessage(), ex);

        entity.setAttempt(attempt +1);
        saveErrorAndPushError(entity.getRequestId(), StatusDeliveryEnum.F24_ERROR, entity, payload -> {
            prepareFlowStarter.redrivePreparePhaseTwoAfterF24Error(entity);
            return null;
        });
    }


    @Override
    public void f24ResponseListener(PnF24PdfSetReadyEvent.Detail body) {
        final String processName = "F24 Response Listener";
        String requestId = body.getPdfSetReady().getRequestId();
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);

        log.logStartingProcess(processName);
        log.info("Received message from F24 queue");
        MDCUtils.addMDCToContextAndExecute(Mono.just(body)
                        .map(msg -> {
                            if (msg==null || StringUtils.isBlank(requestId)) throw new PnGenericException(CORRELATION_ID_NOT_FOUND, CORRELATION_ID_NOT_FOUND.getMessage());
                            else return msg;
                        })
                        .map(PnF24PdfSetReadyEvent.Detail::getPdfSetReady)
                        .flatMap(payload -> f24Service.arrangeF24AttachmentsAndReschedulePrepare(payload.getRequestId(),
                                                payload.getGeneratedPdfsUrls().stream().map(PnF24PdfSetReadyEventItem::getUri).toList()))
                )
                .block();
    }

    @Override
    public void raddAltListener(PnAttachmentsConfigEventPayload data) {
        final String processName = "raddAltListener";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, data.getConfigKey());
        log.logStartingProcess(PROCESS_NAME);
        var monoResult = Mono.just(data)
                .flatMap(request -> attachmentsConfigService.refreshConfig(data))
                .doOnSuccess(resultFromAsync -> log.logEndingProcess(processName))
                .doOnError(ex -> log.error("Error in raddAltListener with configKey: {}", data.getConfigKey(), ex));

        MDCUtils.addMDCToContextAndExecute(monoResult).block();
    }

    @Override
    public void nationalRegistriesResponseListener(AddressSQSMessageDto body) {

        PnLogAudit pnLogAudit = new PnLogAudit();
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, body.getCorrelationId());

        log.logStartingProcess(PROCESS_NAME);
        log.info("Received message from National Registry queue");
        MDCUtils.addMDCToContextAndExecute(Mono.just(body)
                        .map(msg -> {
                            if (msg==null || StringUtils.isBlank(msg.getCorrelationId())) throw new PnGenericException(CORRELATION_ID_NOT_FOUND, CORRELATION_ID_NOT_FOUND.getMessage());
                            else return msg;
                        })
                        .zipWhen(msgDto -> {
                            String correlationId = msgDto.getCorrelationId();
                            log.info("Received message from National Registry queue with correlationId "+correlationId);
                            return requestDeliveryDAO.getByCorrelationId(correlationId)
                                    .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage())));
                        })
                        .doOnNext(addressAndEntity -> resolveAuditLogFromResponse(addressAndEntity.getT2(), addressAndEntity.getT1().getError(), pnLogAudit, PN_NATIONAL_REGISTRIES, addressAndEntity.getT2().getCorrelationId()))

                        /* If Delivery Request status is not NATIONAL_REGISTRY_WAITING then skip to avoid reworks on same or not expected events */
                        .doOnNext(addressAndEntity -> log.info("Skipping National Registry event? {}", !this.isDeliveryRequestInNationalRegistryWaitingStatus(addressAndEntity.getT2())))
                        .filter(addressAndEntity -> this.isDeliveryRequestInNationalRegistryWaitingStatus(addressAndEntity.getT2()))
                        .doOnDiscard(Tuple2.class, addressAndEntity -> log.logEndingProcess(PROCESS_NAME))

                        .flatMap(addressAndEntity -> {
                            AddressSQSMessageDto addressFromNational = addressAndEntity.getT1();
                            PnDeliveryRequest entity = addressAndEntity.getT2();
                            // check error body
                            if (StringUtils.isNotEmpty(addressFromNational.getError())) {
                                log.info("Error message is not empty for correlationId" +addressFromNational.getCorrelationId());

                                PnRequestError pnRequestError = PnRequestError.builder()
                                        .requestId(entity.getRequestId())
                                        .error(NATIONAL_REGISTRY_LISTENER_EXCEPTION.getTitle())
                                        .flowThrow(NATIONAL_REGISTRY_LISTENER_EXCEPTION.getMessage())
                                        .build();

                                paperRequestErrorDAO.created(pnRequestError);

                                throw new PnGenericException(NATIONAL_REGISTRY_LISTENER_EXCEPTION, NATIONAL_REGISTRY_LISTENER_EXCEPTION.getMessage());
                            }

                            return startPrepareAsync(addressFromNational, entity);
                        }))
                .block();
    }

    private Mono<Void> startPrepareAsync(@NotNull AddressSQSMessageDto addressFromNational, PnDeliveryRequest deliveryRequest) {
        Mono<Optional<Address>> addressMono;

        if (validatePhysicalAddressPayload(addressFromNational, deliveryRequest.getRequestId())) {
            Address address = AddressMapper.fromNationalRegistry(addressFromNational.getPhysicalAddress());
            addressMono = this.retrieveRelatedAddress(deliveryRequest.getRelatedRequestId(), address);
        }
        else {
            addressMono = Mono.just(Optional.empty());
        }

        return addressMono
                .doOnNext(updateAddress -> prepareFlowStarter
                        .startPreparePhaseOneFromNationalRegistriesFlow(deliveryRequest, updateAddress.orElse(null)))
                .doOnNext(updateAddress -> log.logEndingProcess(PROCESS_NAME))
                .then();
    }

    private boolean validatePhysicalAddressPayload(AddressSQSMessageDto payload, String requestId) {
        boolean isValid = payload.getPhysicalAddress() != null &&
                payload.getPhysicalAddress().getAddress() != null;

        if(!isValid) {
            log.warn("[{}] Physical Address from NR not valid", requestId);
        }

        return isValid;
    }

    @Override
    public void nationalRegistriesErrorListener(NationalRegistryError data, int attempt) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, data.getRequestId());
        final String processName = "National Registries Error Listener";
        log.logStartingProcess(processName);
        MDCUtils.addMDCToContextAndExecute(Mono.just(data)
                        .doOnSuccess(nationalRegistryError -> {
                            log.info("Called national Registries");
                            log.logInvokingAsyncExternalService(PN_NATIONAL_REGISTRIES,NATIONAL_REGISTRY_DESCRIPTION, nationalRegistryError.getRequestId());
                            nationalRegistryService.finderAddressFromNationalRegistries(
                                    nationalRegistryError.getRequestId(),
                                    nationalRegistryError.getRelatedRequestId(),
                                    nationalRegistryError.getFiscalCode(),
                                    nationalRegistryError.getReceiverType(),
                                    nationalRegistryError.getIun(),
                                    attempt
                            );
                            log.logEndingProcess(processName);
                        })
                        .doOnError(throwable -> {
                            log.error(throwable.getMessage());
                            throw new PnGenericException(NATIONAL_REGISTRY_LISTENER_EXCEPTION, NATIONAL_REGISTRY_LISTENER_EXCEPTION.getMessage());
                        }))
                .block();
    }

    @Override
    public void externalChannelListener(SingleStatusUpdateDto data, int attempt) {
        final String processName = "External Channel Listener";
        if (data.getAnalogMail() != null) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, data.getAnalogMail().getRequestId());
        }

        log.logStartingProcess(processName);


        MDCUtils.addMDCToContextAndExecute(Mono.just(data)
                        .flatMap(request -> this.paperResultAsyncService.resultAsyncBackground(request, attempt))
                        .doOnSuccess(resultFromAsync -> {
                            log.info("End of external Channel result");
                            log.logEndingProcess(processName);
                        })
                        .onErrorResume(ex -> {
                            if (ex instanceof InvalidEventOrderException invalidEventEx
                                    && isDuplicateFeedback(invalidEventEx)) {
                                    log.warn("RequestID: {}, StatusCode: {}, StatusDateTime: {}, " +
                                                    "Message: event with status code {} \n" +
                                                    "has already been processed and feedback has been provided.",
                                            data.getAnalogMail().getRequestId(),
                                            invalidEventEx.getFeedbackStatus().newFeedbackStatusCode(),
                                            invalidEventEx.getFeedbackStatus().newFeedbackStatusDateTime(),
                                            invalidEventEx.getFeedbackStatus().newFeedbackStatusCode());
                                    return Mono.empty(); // Discard the duplicate event
                            }

                            log.error(ex.getMessage());
                            throw new PnGenericException(EXTERNAL_CHANNEL_LISTENER_EXCEPTION, EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage());
                        }))
                .block();

    }

    @Override
    public void manualRetryExternalChannel(String requestId, String newPcRetry) {

        PnLogAudit pnLogAudit = new PnLogAudit();

        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        this.requestDeliveryDAO.getByRequestId(requestId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("prepare requestId {} not existed", requestId);
                    return Mono.empty();
                }))
                .zipWith(this.addressDAO.findAllByRequestId(requestId))
                .flatMap(requestAndAddress ->  {
                    PnDeliveryRequest request = requestAndAddress.getT1();
                    SendRequest sendRequest = SendRequestMapper.toDto(requestAndAddress.getT2(),request);
                    List<AttachmentInfo> attachments = request.getAttachments().stream().map(AttachmentMapper::fromEntity).toList();
                    sendRequest.setRequestId(sendRequest.getRequestId().concat(Const.RETRY).concat(newPcRetry));
                    pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request  to External Channel", sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));
                    return this.externalChannelClient.sendEngageRequest(sendRequest, attachments)
                            .then(Mono.defer(() -> {
                                pnLogAudit.addsSuccessSend(
                                        request.getIun(),
                                        String.format("prepare requestId = %s, trace_id = %s  request  to External Channel", sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY))
                                );
                                RequestDeliveryMapper.changeState(
                                        request,
                                        READY_TO_SEND.getCode(),
                                        READY_TO_SEND.getDescription(),
                                        READY_TO_SEND.getDetail(),
                                        request.getProductType(),
                                        null);
                                return this.requestDeliveryDAO.updateData(request);
                            }))
                            .onErrorResume(ex -> {
                                log.warn("[{}] Error in manualRetryExternalChannel", requestId, ex);
                                pnLogAudit.addsWarningSend(
                                        request.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", request.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));

                                PnRequestError pnRequestError = PnRequestError.builder()
                                        .requestId(requestId)
                                        .error(ex.getMessage())
                                        .flowThrow(EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                                        .build();

                                return paperRequestErrorDAO.created(pnRequestError)
                                        .flatMap(errorEntity -> Mono.error(ex));
                            });
                })
                .block();
    }

    @Override
    public void delayerListener(PnPrepareDelayerToPaperchannelPayload data, int attempt) {
        String processName = "DelayerListener";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, data.getRequestId());
        log.logStartingProcess(processName);
        MDCUtils.addMDCToContextAndExecute(Mono.just(data)
                        .flatMap(delayerRequest -> {
                            delayerRequest.setAttempt(attempt);
                            return this.preparePhaseTwoAsyncService.prepareAsyncPhaseTwo(data);
                        })
                        .doOnSuccess(resultFromAsync ->{
                                    log.info("End of prepare async internal");
                                    log.logEndingProcess(processName);
                                }
                        )
                        .doOnError(throwable -> {
                            log.error(throwable.getMessage());
                            throw new PnGenericException(PREPARE_ASYNC_LISTENER_EXCEPTION, PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage());
                        }))
                .block();
    }


    private Mono<Optional<Address>> retrieveRelatedAddress(String relatedRequestId, Address fromNationalRegistry){
        return addressDAO.findByRequestId(relatedRequestId)
                .map(address -> {
                    fromNationalRegistry.setFullName(address.getFullName());
                    return Optional.of(fromNationalRegistry);
                }).switchIfEmpty(Mono.just(Optional.of(fromNationalRegistry)));

    }

    private boolean isDeliveryRequestInNationalRegistryWaitingStatus(PnDeliveryRequest deliveryRequest) {
        return NATIONAL_REGISTRY_WAITING.getCode().equalsIgnoreCase(deliveryRequest.getStatusCode());
    }

    /**
     * Checks if an InvalidEventOrderException represents a duplicate event.
     *
     * @param invalidEventEx the InvalidEventOrderException to check
     * @return true if the exception indicates a duplicate event, false otherwise.
     */
    private static boolean isDuplicateFeedback(InvalidEventOrderException invalidEventEx){
        if (invalidEventEx == null || invalidEventEx.getFeedbackStatus() == null) {
            return false; // If invalidEventEx or feedbackStatus is null, return false
        }

        FeedbackStatus feedback = invalidEventEx.getFeedbackStatus();
        return invalidEventEx.getExceptionType() == ExceptionTypeEnum.WRONG_EVENT_ORDER
                && Objects.equals(feedback.oldFeedbackStatusCode(), feedback.newFeedbackStatusCode())
                && Objects.equals(feedback.oldFeedbackDeliveryFailureCause(), feedback.newFeedbackDeliveryFailureCause())
                && Objects.equals(feedback.oldFeedbackStatusDateTime(), feedback.newFeedbackStatusDateTime());
    }
}
