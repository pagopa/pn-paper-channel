package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

// Stato IN PROGRESS per paper-channel e delivery-push
@Slf4j
public class ForwardProgressMessageHandler extends SendToDeliveryPushHandler {

    public ForwardProgressMessageHandler(SqsSender sqsSender) {
        super(sqsSender);
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] ForwardProgress handler start status={}", paperRequest.getRequestId(), paperRequest.getStatusCode());
        return super.handleMessage(entity, paperRequest);
    }
}
