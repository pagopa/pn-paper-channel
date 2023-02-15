package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.Single;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Date;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@Slf4j
@Service
public class PaperResultAsyncServiceImpl extends BaseService implements PaperResultAsyncService {

    @Autowired
    private ExternalChannelClient externalChannelClient;
    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final AddressDAO addressDAO;
    private final PaperRequestErrorDAO paperRequestErrorDAO;

    public PaperResultAsyncServiceImpl(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO,
                                       NationalRegistryClient nationalRegistryClient, SqsSender sqsSender, PnPaperChannelConfig pnPaperChannelConfig, AddressDAO addressDAO, PaperRequestErrorDAO paperRequestErrorDAO) {
        super(auditLogBuilder, requestDeliveryDAO, null, nationalRegistryClient, sqsSender);
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.addressDAO = addressDAO;
        this.paperRequestErrorDAO = paperRequestErrorDAO;
    }

    @Override
    public Mono<PnDeliveryRequest> resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt) {
        if (singleStatusUpdateDto == null || singleStatusUpdateDto.getAnalogMail() == null || StringUtils.isBlank(singleStatusUpdateDto.getAnalogMail().getRequestId())){
            log.error("the message sent from external channel, includes errors. It cannot be processing");
            return Mono.error(new PnGenericException(DATA_NULL_OR_INVALID, DATA_NULL_OR_INVALID.getMessage()));
        }

        String requestId = getPrefixRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId());
        return requestDeliveryDAO.getByRequestId(requestId)
                .flatMap(entity -> {
                    log.info("GETTED ENTITY: {}", entity.getRequestId());
                    SingleStatusUpdateDto logDto = singleStatusUpdateDto;
                    logDto.getAnalogMail().setDiscoveredAddress(null);
                    pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel", entity.getRequestId()));
                    pnLogAudit.addsSuccessReceive(entity.getIun(), String.format("prepare requestId = %s Response %s from external-channel status code %s",  entity.getRequestId(), logDto, entity.getStatusCode()));

                    return updateEntityResult(singleStatusUpdateDto, entity)
                            .flatMap(updatedEntity -> {
                                log.info("UPDATED ENTITY: {}", updatedEntity.getRequestId());
                                if (isRetryStatusCode(singleStatusUpdateDto)) {
                                    sendEngageRequest(updatedEntity, setRetryRequestId(updatedEntity.getRequestId()));
                                }
                                sendPaperResponse(updatedEntity, singleStatusUpdateDto);
                                return Mono.just(updatedEntity);
                            })
                            .onErrorResume(ex -> {
                                //TODO case of retry event from external-channel queue
                                ex.printStackTrace();
                                return Mono.error(ex);
                            });
                });
    }

    private String getPrefixRequestId(String requestId) {
        if (requestId.contains(Const.RETRY)) {
            requestId = requestId.substring(0, requestId.indexOf(Const.RETRY));
        }
        return requestId;
    }

    private boolean isRetryStatusCode(SingleStatusUpdateDto singleStatusUpdateDto) {
        boolean retryStatusCod = false;
        if (singleStatusUpdateDto.getAnalogMail() != null && StringUtils.equals(singleStatusUpdateDto.getAnalogMail().getStatusCode(), this.pnPaperChannelConfig.getRetryStatus())) {
            retryStatusCod = true;
        }
        return retryStatusCod;
    }

    private String setRetryRequestId(String requestId) {
        String rertyReqId = getPrefixRequestId(requestId);
        if (requestId.contains(Const.RETRY)) {
            String attempt = String.valueOf(Integer.parseInt(requestId.substring(requestId.indexOf(Const.RETRY), requestId.length()-1))+1);
            rertyReqId = rertyReqId.concat(Const.RETRY).concat(attempt);
        }
        return rertyReqId;
    }

    private void sendEngageRequest(PnDeliveryRequest pnDeliveryRequest, String requestId) {
        Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                .flatMap(i ->  addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId()))
                .map(pnAddresses -> {
                    SendRequest sendRequest = SendRequestMapper.toDto(pnAddresses, pnDeliveryRequest);
                    sendRequest.setRequestId(requestId);
                    return this.externalChannelClient.sendEngageRequest(sendRequest)
                            .then(Mono.defer(() -> {
                                pnLogAudit.addsSuccessSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                                return Mono.empty();
                            }))
                            .onErrorResume(ex -> {
                                pnLogAudit.addsFailSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                                paperRequestErrorDAO.created(sendRequest.getRequestId(),
                                        EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(),
                                        EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name());
                                return Mono.error(ex);
                            });
                })
                .publishOn(Schedulers.boundedElastic())
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private Mono<PnDeliveryRequest> updateEntityResult(SingleStatusUpdateDto singleStatusUpdateDto, PnDeliveryRequest pnDeliveryRequestMono) {
        pnDeliveryRequestMono.setStatusCode(singleStatusUpdateDto.getAnalogMail().getStatusCode());
        pnDeliveryRequestMono.setStatusDetail(singleStatusUpdateDto.getAnalogMail().getStatusDescription());
        pnDeliveryRequestMono.setStatusDate(DateUtils.formatDate(Date.from(singleStatusUpdateDto.getAnalogMail().getStatusDateTime().toInstant())));
        return requestDeliveryDAO.updateData(pnDeliveryRequestMono);
    }

    private void sendPaperResponse(PnDeliveryRequest entity, SingleStatusUpdateDto request) {
        SendEvent sendEvent = new SendEvent();

        sendEvent.setStatusCode(entity.getStatusCode());
        sendEvent.setStatusDetail(entity.getStatusDetail());
        sendEvent.setStatusDescription(entity.getStatusDetail());

        if (request.getAnalogMail() != null) {
            sendEvent.setRequestId(request.getAnalogMail().getRequestId());
            sendEvent.setStatusDateTime(DateUtils.getDatefromOffsetDateTime(request.getAnalogMail().getStatusDateTime()));
            sendEvent.setRegisteredLetterCode(request.getAnalogMail().getRegisteredLetterCode());
            sendEvent.setClientRequestTimeStamp(Date.from(request.getAnalogMail().getClientRequestTimeStamp().toInstant()));
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
    }

}
