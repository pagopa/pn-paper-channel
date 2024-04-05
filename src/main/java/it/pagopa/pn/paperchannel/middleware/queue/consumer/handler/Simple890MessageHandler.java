package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@SuperBuilder
@Slf4j
public class Simple890MessageHandler extends SendToDeliveryPushHandler {

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return super.handleMessage(SendEventMapper.changeToProgressStatus(entity), paperRequest);
    }
}
