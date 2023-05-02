package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import reactor.core.publisher.Mono;

public interface MessageHandler {

    Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest);
}
