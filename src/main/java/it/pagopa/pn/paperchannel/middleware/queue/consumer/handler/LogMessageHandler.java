package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

// handler solo log
@Slf4j
public class LogMessageHandler implements MessageHandler {


    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("Message not handled: {}", paperRequest);
        return Mono.empty();
    }

}
