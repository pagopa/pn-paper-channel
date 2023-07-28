package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.HandlersFactory;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.MessageHandler;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DATA_NULL_OR_INVALID;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_API_EXCEPTION;

@CustomLog
@Service
public class PaperResultAsyncServiceImpl extends BaseService implements PaperResultAsyncService {

    private final ExternalChannelClient externalChannelClient;
    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final AddressDAO addressDAO;
    private final PaperRequestErrorDAO paperRequestErrorDAO;

    private final HandlersFactory handlersFactory;

    private final String processName = "Result Async Background";

    public PaperResultAsyncServiceImpl(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO,
                                       NationalRegistryClient nationalRegistryClient, SqsSender sqsSender,
                                       PnPaperChannelConfig pnPaperChannelConfig, AddressDAO addressDAO,
                                       PaperRequestErrorDAO paperRequestErrorDAO, HandlersFactory handlersFactory,
                                       ExternalChannelClient externalChannelClient) {
        super(auditLogBuilder, requestDeliveryDAO, null, nationalRegistryClient, sqsSender);
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.addressDAO = addressDAO;
        this.paperRequestErrorDAO = paperRequestErrorDAO;
        this.handlersFactory = handlersFactory;
        this.externalChannelClient = externalChannelClient;
    }

    //TODO cancellare questo metodo inutilizzato
//    @Override
    public Mono<PnDeliveryRequest> resultAsyncBackgroundOld(SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt) {
        if (singleStatusUpdateDto == null || singleStatusUpdateDto.getAnalogMail() == null || StringUtils.isBlank(singleStatusUpdateDto.getAnalogMail().getRequestId())){
            log.error("the message sent from external channel, includes errors. It cannot be processing");
            return Mono.error(new PnGenericException(DATA_NULL_OR_INVALID, DATA_NULL_OR_INVALID.getMessage()));
        }

        String requestId = getPrefixRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId());
        return requestDeliveryDAO.getByRequestId(requestId)
                .flatMap(entity -> {
                    log.info("GETTED ENTITY: {}", entity.getRequestId());
                    SingleStatusUpdateDto logDto = singleStatusUpdateDto;
                    DiscoveredAddressDto discoveredAddressDto = logDto.getAnalogMail().getDiscoveredAddress();
                    logDto.getAnalogMail().setDiscoveredAddress(null);
                    pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel", entity.getRequestId()));
                    pnLogAudit.addsSuccessReceive(entity.getIun(), String.format("prepare requestId = %s Response %s from external-channel status code %s",  entity.getRequestId(), logDto.toString().replaceAll("\n", ""), entity.getStatusCode()));
                    logDto.getAnalogMail().setDiscoveredAddress(discoveredAddressDto);
                    return updateEntityResult(singleStatusUpdateDto, entity)
                            .publishOn(Schedulers.boundedElastic())
                            .flatMap(updatedEntity -> {
                                log.info("UPDATED ENTITY: {}", updatedEntity.getRequestId());
                                if (isRetryStatusCode(singleStatusUpdateDto)) {
                                    if (hasOtherAttempt(singleStatusUpdateDto.getAnalogMail().getRequestId())) {
                                        sendEngageRequest(updatedEntity, setRetryRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId()));
                                    } else {
                                        Mono.delay(Duration.ofMillis(1)).publishOn(Schedulers.boundedElastic())
                                                        .flatMap( i-> paperRequestErrorDAO.created(entity.getRequestId(),
                                                                EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(),
                                                                EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name()).map(item -> item));
                                    }

                                } else if (isTechnicalErrorStatusCode(singleStatusUpdateDto)) {
                                    PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);
                                    pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry to External Channel", entity.getRequestId()));
                                    return Mono.delay(Duration.ofMillis(1)).publishOn(Schedulers.boundedElastic())
                                            .flatMap( i-> paperRequestErrorDAO.created(entity.getRequestId(),
                                                    entity.getStatusCode(),
                                                    entity.getStatusDetail()).map(item -> item))
                                            .flatMap(item -> {
                                                pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry to External Channel", entity.getRequestId()));
                                                return Mono.just(updatedEntity);
                                            });
                                }
                                if (!isTechnicalErrorStatusCode(singleStatusUpdateDto)) sendPaperResponse(updatedEntity, singleStatusUpdateDto);
                                return Mono.just(updatedEntity);
                            })
                            .onErrorResume(ex -> {
                                log.error("Error in retrieve EC from queue {}", ex.getMessage());
                                return Mono.error(ex);
                            });
                });
    }

    @Override
    public Mono<Void> resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt) {
        log.logStartingProcess(processName);
        if (singleStatusUpdateDto == null || singleStatusUpdateDto.getAnalogMail() == null || StringUtils.isBlank(singleStatusUpdateDto.getAnalogMail().getRequestId())) {
            log.error("the message sent from external channel, includes errors. It cannot be processing");
            return Mono.error(new PnGenericException(DATA_NULL_OR_INVALID, DATA_NULL_OR_INVALID.getMessage()));
        }

        if(singleStatusUpdateDto.getAnalogMail().getStatusCode().equals("P000") ){
            log.debug("Received P000 from EC for {}", singleStatusUpdateDto.getAnalogMail().getRequestId());
            return Mono.empty();
        }

        MessageHandler handler = handlersFactory.getHandler(singleStatusUpdateDto.getAnalogMail().getStatusCode());

        String requestId = getPrefixRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId());
        return requestDeliveryDAO.getByRequestId(requestId)
                .doOnNext(entity -> logEntity(entity, singleStatusUpdateDto))
                .flatMap(entity -> updateEntityResult(singleStatusUpdateDto, entity))
                .flatMap(entity -> handler.handleMessage(entity, singleStatusUpdateDto.getAnalogMail()))
                .doOnError(ex ->  log.error("Error in retrieve EC from queue", ex));

    }

    private void logEntity(PnDeliveryRequest entity, SingleStatusUpdateDto singleStatusUpdateDto) {
        log.info("GETTED ENTITY: {}", entity.getRequestId());
        SingleStatusUpdateDto logDto = singleStatusUpdateDto;
        DiscoveredAddressDto discoveredAddressDto = logDto.getAnalogMail().getDiscoveredAddress();
        logDto.getAnalogMail().setDiscoveredAddress(null);
        pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel", entity.getRequestId()));
        pnLogAudit.addsSuccessReceive(entity.getIun(), String.format("prepare requestId = %s Response %s from external-channel status code %s", entity.getRequestId(), logDto.toString().replaceAll("\n", ""), entity.getStatusCode()));
        logDto.getAnalogMail().setDiscoveredAddress(discoveredAddressDto);
    }

    private String getPrefixRequestId(String requestId) {
        if (requestId.contains(Const.RETRY)) {
            requestId = requestId.substring(0, requestId.indexOf(Const.RETRY));
        }
        return requestId;
    }

    private boolean isRetryStatusCode(SingleStatusUpdateDto singleStatusUpdateDto) {
        boolean retryStatusCod = false;
        if (singleStatusUpdateDto.getAnalogMail() != null && ExternalChannelCodeEnum.isRetryStatusCode(singleStatusUpdateDto.getAnalogMail().getStatusCode())) {
            retryStatusCod = true;
        }
        return retryStatusCod;
    }

    private boolean isTechnicalErrorStatusCode(SingleStatusUpdateDto singleStatusUpdateDto) {
        boolean retryStatusCod = false;
        if (singleStatusUpdateDto.getAnalogMail() != null && ExternalChannelCodeEnum.isErrorStatusCode(singleStatusUpdateDto.getAnalogMail().getStatusCode())) {
            retryStatusCod = true;
        }
        return retryStatusCod;
    }

    private boolean hasOtherAttempt(String requestId) {
        return pnPaperChannelConfig.getAttemptQueueExternalChannel() != -1 || pnPaperChannelConfig.getAttemptQueueExternalChannel() < getRetryAttempt(requestId);
    }

    private String setRetryRequestId(String requestId) {
        String rertyReqId = getPrefixRequestId(requestId);
        if (requestId.contains(Const.RETRY)) {
            String attempt = String.valueOf(getRetryAttempt(requestId)+1);
            rertyReqId = rertyReqId.concat(Const.RETRY).concat(attempt);
        }
        return rertyReqId;
    }

    private int getRetryAttempt(String requestId) {
        int retry = 0;
        if (requestId.contains(Const.RETRY)) {
            retry = Integer.parseInt(requestId.substring(requestId.lastIndexOf("_")+1));
        }
        return retry;
    }

    private void sendEngageRequest(PnDeliveryRequest pnDeliveryRequest, String requestId) {
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        MDCUtils.addMDCToContextAndExecute(Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                .flatMap(i ->  addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId()))
                .flatMap(pnAddresses -> {
                    SendRequest sendRequest = SendRequestMapper.toDto(pnAddresses, pnDeliveryRequest);
                    sendRequest.setRequestId(requestId);
                    pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));
                    return this.externalChannelClient.sendEngageRequest(sendRequest, pnDeliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList()).publishOn(Schedulers.boundedElastic())
                            .then(Mono.defer(() -> {
                                pnLogAudit.addsSuccessSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));
                                return Mono.empty();
                            }))
                            .onErrorResume(ex -> {
                                pnLogAudit.addsWarningSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));
                                return paperRequestErrorDAO.created(sendRequest.getRequestId(),
                                        EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(),
                                        EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name()).flatMap(errorEntity -> Mono.error(ex));
                            });
                })
                .subscribeOn(Schedulers.boundedElastic())).subscribe();
    }

    private Mono<PnDeliveryRequest> updateEntityResult(SingleStatusUpdateDto singleStatusUpdateDto, PnDeliveryRequest pnDeliveryRequestMono) {
        RequestDeliveryMapper.changeState(
                pnDeliveryRequestMono,
                singleStatusUpdateDto.getAnalogMail().getStatusCode(),
                singleStatusUpdateDto.getAnalogMail().getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(singleStatusUpdateDto.getAnalogMail().getStatusCode()),
                pnDeliveryRequestMono.getProductType(),
                singleStatusUpdateDto.getAnalogMail().getStatusDateTime().toInstant()
        );
        log.logEndingProcess(processName);
        return requestDeliveryDAO.updateData(pnDeliveryRequestMono);
    }

    private void sendPaperResponse(PnDeliveryRequest entity, SingleStatusUpdateDto request) {
        SendEvent sendEvent = new SendEvent();

        try {
            sendEvent.setStatusCode(StatusCodeEnum.valueOf(entity.getStatusCode()));
            sendEvent.setStatusDetail(entity.getStatusDetail());
            sendEvent.setStatusDescription(entity.getStatusDetail());

            if (request.getAnalogMail() != null) {
                sendEvent.setRequestId(entity.getRequestId());
                sendEvent.setStatusDateTime(DateUtils.getDatefromOffsetDateTime(request.getAnalogMail().getStatusDateTime()));
                sendEvent.setRegisteredLetterCode(request.getAnalogMail().getRegisteredLetterCode());
                sendEvent.setClientRequestTimeStamp(request.getAnalogMail().getClientRequestTimeStamp().toInstant());
                sendEvent.setDeliveryFailureCause(request.getAnalogMail().getDeliveryFailureCause());
                sendEvent.setDiscoveredAddress(AddressMapper.toPojo(request.getAnalogMail().getDiscoveredAddress()));

                if (request.getAnalogMail().getAttachments() != null && !request.getAnalogMail().getAttachments().isEmpty()) {
                    sendEvent.setAttachments(
                            request.getAnalogMail().getAttachments()
                                    .stream()
                                    .map(AttachmentMapper::fromAttachmentDetailsDto)
                                    .toList()
                    );
                }
            }

            sqsSender.pushSendEvent(sendEvent);
        } catch (IllegalArgumentException ex){
            log.info(ex.getMessage());
        }


    }

}
