package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public abstract class SendToDeliveryPushHandler implements MessageHandler {

    protected final SqsSender sqsSender;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] Sending to delivery-push or event-bridge", paperRequest.getRequestId());
        log.debug("[{}] Response of ExternalChannel from request id {}", paperRequest.getRequestId(), paperRequest);
        SendEvent sendEvent = SendEventMapper.createSendEventMessage(entity, paperRequest);

        if (entity.getRequestId().contains(Const.PREFIX_REQUEST_ID_SERVICE_DESK)){
            String clientId = MDC.get(Const.CONTEXT_KEY_CLIENT_ID);
            log.debug("[{}] clientId from context", clientId);
            sqsSender.pushSendEventOnEventBridge(clientId, sendEvent);
            log.info("[{}] Sent to event-bridge: {}", paperRequest.getRequestId(), sendEvent);
        } else {
            sqsSender.pushSendEvent(sendEvent);
            log.info("[{}] Sent to delivery-push: {}", paperRequest.getRequestId(), sendEvent);
        }
        return Mono.empty();
    }

}
