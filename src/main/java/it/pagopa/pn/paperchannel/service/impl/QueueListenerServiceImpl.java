package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEvent;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEventItem;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.commons.log.PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.READY_TO_SEND;

@Service
@CustomLog
public class QueueListenerServiceImpl extends BaseService implements QueueListenerService {
    private static final String NATIONAL_REGISTRY_DESCRIPTION = "Retrieve the address.";

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;
    @Autowired
    private PaperAsyncService paperAsyncService;
    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private PaperRequestErrorDAO paperRequestErrorDAO;
    @Autowired
    private ExternalChannelClient externalChannelClient;
    @Autowired
    private F24Service f24Service;
    @Autowired
    private DematZipService dematZipService;

    public QueueListenerServiceImpl(PnAuditLogBuilder auditLogBuilder,
                                    RequestDeliveryDAO requestDeliveryDAO,
                                    CostDAO costDAO,
                                    NationalRegistryClient nationalRegistryClient,
                                    SqsSender sqsSender) {
        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsSender);
    }



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
                            log.error(throwable.getMessage());
                            //TODO capire cosa fare in caso di errore
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
            log.info("attempting to pushing to internal payload={}", payload);
            sqsSender.pushInternalError(payload, entity.getAttempt(), F24Error.class);
            return null;
        });
    }


    @Override
    public void f24ResponseListener(PnF24PdfSetReadyEvent.Detail body) {
        final String PROCESS_NAME = "F24 Response Listener";
        String requestId = body.getPdfSetReady().getRequestId();
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);

        log.logStartingProcess(PROCESS_NAME);
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
    public void nationalRegistriesResponseListener(AddressSQSMessageDto body) {
        final String PROCESS_NAME = "National Registries Response Listener";
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
                        .doOnNext(addressAndEntity -> nationalResolveLogAudit(addressAndEntity.getT2(), addressAndEntity.getT1()))
                        .flatMap(addressAndEntity -> {
                            AddressSQSMessageDto addressFromNational = addressAndEntity.getT1();
                            PnDeliveryRequest entity = addressAndEntity.getT2();
                            // check error body
                            if (StringUtils.isNotEmpty(addressFromNational.getError())) {
                                log.info("Error message is not empty for correlationId" +addressFromNational.getCorrelationId());
                                paperRequestErrorDAO.created(entity.getRequestId(), NATIONAL_REGISTRY_LISTENER_EXCEPTION.getTitle(), NATIONAL_REGISTRY_LISTENER_EXCEPTION.getMessage());
                                throw new PnGenericException(NATIONAL_REGISTRY_LISTENER_EXCEPTION, NATIONAL_REGISTRY_LISTENER_EXCEPTION.getMessage());
                            }

                            if (addressFromNational.getPhysicalAddress() != null) {
                                Address address = AddressMapper.fromNationalRegistry(addressFromNational.getPhysicalAddress());
                                return this.retrieveRelatedAddress(entity.getRelatedRequestId(), address)
                                        .map(updateAddress -> new PrepareAsyncRequest(entity.getCorrelationId(), updateAddress));
                            }

                            return Mono.just(new PrepareAsyncRequest(addressAndEntity.getT2().getCorrelationId(), null));
                        })
                        .flatMap(prepareRequest -> {
                            this.sqsSender.pushToInternalQueue(prepareRequest);
                            log.logEndingProcess(PROCESS_NAME);
                            return Mono.empty();
                        }))
                .block();
    }

    @Override
    public void nationalRegistriesErrorListener(NationalRegistryError data, int attempt) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, data.getRequestId());
        final String PROCESS_NAME = "National Registries Error Listener";
        log.logStartingProcess(PROCESS_NAME);
        MDCUtils.addMDCToContextAndExecute(Mono.just(data)
                        .doOnSuccess(nationalRegistryError -> {
                            log.info("Called national Registries");
                            log.logInvokingAsyncExternalService(PN_NATIONAL_REGISTRIES,NATIONAL_REGISTRY_DESCRIPTION, nationalRegistryError.getRequestId());
                            this.finderAddressFromNationalRegistries(
                                    nationalRegistryError.getCorrelationId(),
                                    nationalRegistryError.getRequestId(),
                                    nationalRegistryError.getRelatedRequestId(),
                                    nationalRegistryError.getFiscalCode(),
                                    nationalRegistryError.getReceiverType(),
                                    nationalRegistryError.getIun(),
                                    attempt
                            );
                            log.logEndingProcess(PROCESS_NAME);
                        })
                        .doOnError(throwable -> {
                            log.error(throwable.getMessage());
                            throw new PnGenericException(NATIONAL_REGISTRY_LISTENER_EXCEPTION, NATIONAL_REGISTRY_LISTENER_EXCEPTION.getMessage());
                        }))
                .block();
    }

    @Override
    public void externalChannelListener(SingleStatusUpdateDto data, int attempt) {
        final String PROCESS_NAME = "External Channel Listener";
        if (data.getAnalogMail() != null) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, data.getAnalogMail().getRequestId());
        }

        log.logStartingProcess(PROCESS_NAME);


        MDCUtils.addMDCToContextAndExecute(Mono.just(data)
                        .flatMap(request -> this.paperResultAsyncService.resultAsyncBackground(request, attempt))
                        .doOnSuccess(resultFromAsync -> {
                            log.info("End of external Channel result");
                            log.logEndingProcess(PROCESS_NAME);
                        })
                        .onErrorResume(ex -> {
                            log.error(ex.getMessage());
                            throw new PnGenericException(EXTERNAL_CHANNEL_LISTENER_EXCEPTION, EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage());
                        }))
                .block();

    }

    @Override
    public void manualRetryExternalChannel(String requestId, String newPcRetry) {
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
                                pnLogAudit.addsWarningSend(
                                        request.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", request.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));
                                return paperRequestErrorDAO.created(requestId, EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(), EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                                        .flatMap(errorEntity -> Mono.error(ex));
                            });
                })
                .block();
    }


    private Mono<Address> retrieveRelatedAddress(String relatedRequestId, Address fromNationalRegistry){
        return addressDAO.findByRequestId(relatedRequestId)
                .map(address -> {
                    fromNationalRegistry.setFullName(address.getFullName());
                    return fromNationalRegistry;
                }).switchIfEmpty(Mono.just(fromNationalRegistry));

    }


    private void nationalResolveLogAudit(PnDeliveryRequest entity, AddressSQSMessageDto address) {
        if (StringUtils.isEmpty(address.getError())) {
            pnLogAudit.addsSuccessResolveService(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, traceId = %s Response OK from National Registry service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            entity.getCorrelationId()));
        } else {
            pnLogAudit.addsFailResolveService(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, traceId = %s Response KO from National Registry service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            entity.getCorrelationId()));
        }
    }

}
