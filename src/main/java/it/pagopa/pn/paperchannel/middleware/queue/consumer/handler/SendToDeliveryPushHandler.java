package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

@Slf4j
@SuperBuilder
public abstract class SendToDeliveryPushHandler implements MessageHandler {

    protected final SqsSender sqsSender;
    protected final RequestDeliveryDAO requestDeliveryDAO;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return this.updateRefinedDeliveryRequestIfOK(entity)
                .doOnNext(pnDeliveryRequest -> this.pushSendEvent(entity, paperRequest))
                .then();
    }

    /**
     * Update delivery request setting refined field to true when statusDetail field is OK
     *
     * @param pnDeliveryRequest request to update
     *
     * @return Mono containing {@link PnDeliveryRequest} object
     * */
    private Mono<PnDeliveryRequest> updateRefinedDeliveryRequestIfOK(PnDeliveryRequest pnDeliveryRequest) {

        if (StatusCodeEnum.OK.getValue().equals(pnDeliveryRequest.getStatusDetail())) {
            log.debug("[{}] Updating DeliveryRequest with refinement information", pnDeliveryRequest.getRequestId());

            pnDeliveryRequest.setRefined(true);
            return this.requestDeliveryDAO
                    .updateData(pnDeliveryRequest)
                    .doOnError(ex -> log.warn("[{}] Error while setting request as refined", pnDeliveryRequest.getRequestId(), ex));
        }

        return Mono.just(pnDeliveryRequest);
    }

    /**
     * Send event to event bridge or delivery push based on delivery request id
     *
     * @param pnDeliveryRequest delivery request
     * @param paperRequest      progress event coming from ext-channel
     * */
    private void pushSendEvent(PnDeliveryRequest pnDeliveryRequest, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] Sending to delivery-push or event-bridge", paperRequest.getRequestId());
        log.debug("[{}] Response of ExternalChannel from request id {}", paperRequest.getRequestId(), paperRequest);

        SendEvent sendEvent = SendEventMapper.createSendEventMessage(pnDeliveryRequest, paperRequest);

        if (Utility.isCallCenterEvoluto(pnDeliveryRequest.getRequestId())){
            String clientId = MDC.get(Const.CONTEXT_KEY_CLIENT_ID);
            log.debug("[{}] clientId from context", clientId);
            sqsSender.pushSendEventOnEventBridge(clientId, sendEvent);
            log.info("[{}] Sent to event-bridge: {}", paperRequest.getRequestId(), sendEvent);
        } else {
            sqsSender.pushSendEvent(sendEvent);
            log.info("[{}] Sent to delivery-push: {}", paperRequest.getRequestId(), sendEvent);
        }
    }
}
