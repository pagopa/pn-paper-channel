package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

// Stato IN PROGRESS per paper-channel e delivery-push
@Slf4j
public class CON080MessageHandler extends SendToDeliveryPushHandler {

    public CON080MessageHandler(SqsSender sqsSender) {
        super(sqsSender);
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] CON080 handler start", paperRequest.getRequestId());
        return super.handleMessage(entity, paperRequest);
    }
}
