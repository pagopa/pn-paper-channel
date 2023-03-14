package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import reactor.core.publisher.Mono;

public class DirectlySendMessageHandler extends SendToDeliveryPushHandler {
    public DirectlySendMessageHandler(SqsSender sqsSender) {
        super(sqsSender);
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        super.handleMessage(entity, paperRequest); // invio diretto dato su delivery-push

        return Mono.empty();
    }
}
