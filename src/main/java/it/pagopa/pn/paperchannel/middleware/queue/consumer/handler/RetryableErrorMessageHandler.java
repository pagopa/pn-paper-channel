package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_API_EXCEPTION;

// handler per stati gialli: Retry su ExtCh con suffisso +1, invio dello stato in progress verso DP
@Slf4j
//@RequiredArgsConstructor
public class RetryableErrorMessageHandler extends SendToDeliveryPushHandler {

    private final ExternalChannelClient externalChannelClient;

    private final AddressDAO addressDAO;

    private final PaperRequestErrorDAO paperRequestErrorDAO;

    private final PnPaperChannelConfig pnPaperChannelConfig;

    public RetryableErrorMessageHandler(SqsSender sqsSender, ExternalChannelClient externalChannelClient,
                                        AddressDAO addressDAO, PaperRequestErrorDAO paperRequestErrorDAO,
                                        PnPaperChannelConfig pnPaperChannelConfig) {
        super(sqsSender);
        this.externalChannelClient = externalChannelClient;
        this.addressDAO = addressDAO;
        this.paperRequestErrorDAO = paperRequestErrorDAO;
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }


    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        if (hasOtherAttempt(paperRequest.getRequestId())) {
            //invio di nuovo la richiesta a ext-channels
            return sendEngageRequest(entity, paperRequest.getRequestId())
                    .flatMap(pnDeliveryRequest -> super.handleMessage(entity, paperRequest));
        } else {
            return paperRequestErrorDAO.created(entity.getRequestId(),
                            EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(), EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                    .flatMap(pnRequestError -> super.handleMessage(entity, paperRequest));
        }

    }

    private boolean hasOtherAttempt(String requestId) {
        return pnPaperChannelConfig.getAttemptQueueExternalChannel() != -1 || pnPaperChannelConfig.getAttemptQueueExternalChannel() < getRetryAttempt(requestId);
    }

    private int getRetryAttempt(String requestId) {
        int retry = 0;
        if (requestId.contains(Const.RETRY)) {
            retry = Integer.parseInt(requestId.substring(requestId.lastIndexOf("_")+1));
        }
        return retry;
    }

    private Mono<PnDeliveryRequest> sendEngageRequest(PnDeliveryRequest pnDeliveryRequest, String requestId) {

        return addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId())
                .flatMap(pnAddresses -> callExternalChannel(pnAddresses, pnDeliveryRequest, requestId));

    }

    private Mono<PnDeliveryRequest> callExternalChannel(List<PnAddress> pnAddresses, PnDeliveryRequest pnDeliveryRequest, String requestId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);

        SendRequest sendRequest = SendRequestMapper.toDto(pnAddresses, pnDeliveryRequest);
        sendRequest.setRequestId(requestId);
        pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));

        List<AttachmentInfo> attachmentInfos = pnDeliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList();

        return externalChannelClient.sendEngageRequest(sendRequest, attachmentInfos)
                .doOnSuccess(unused -> pnLogAudit.addsSuccessSend(sendRequest.getIun(),
                        String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)))
                )
                .onErrorResume(ex -> {
                    pnLogAudit.addsWarningSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                    return paperRequestErrorDAO.created(sendRequest.getRequestId(), EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(),
                                    EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                            .flatMap(errorEntity -> Mono.error(ex));
                })
                .thenReturn(pnDeliveryRequest);

    }

    //TODO cancellare questo metodo inutilizzato
    private void sendEngageRequestOld(PnDeliveryRequest pnDeliveryRequest, String requestId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);

        Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                .flatMap(i ->  addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId()))
                .flatMap(pnAddresses -> {
                    SendRequest sendRequest = SendRequestMapper.toDto(pnAddresses, pnDeliveryRequest);
                    sendRequest.setRequestId(requestId);
                    pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));

                    return this.externalChannelClient.sendEngageRequest(sendRequest, pnDeliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList()).publishOn(Schedulers.boundedElastic())
                            .then(Mono.defer(() -> {
                                pnLogAudit.addsSuccessSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                                return Mono.empty();
                            }))
                            .onErrorResume(ex -> {
                                pnLogAudit.addsFailSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                                return paperRequestErrorDAO.created(sendRequest.getRequestId(),
                                        EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(),
                                        EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name()).flatMap(errorEntity -> Mono.error(ex));
                            });
                })
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

}
