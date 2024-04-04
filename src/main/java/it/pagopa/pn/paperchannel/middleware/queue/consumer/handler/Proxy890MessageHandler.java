package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Builder
@Slf4j
public class Proxy890MessageHandler implements MessageHandler {

    private final Complex890MessageHandler complex890MessageHandler;
    private final PnPaperChannelConfig pnPaperChannelConfig;

    @PostConstruct
    public void postConstruct(){
        log.info("Init Proxy890MessageHandler - complexRefinementCodes: {} , enableSimple890Flow: {}",
                pnPaperChannelConfig.getComplexRefinementCodes(),pnPaperChannelConfig.isEnableSimple890Flow());
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.info("Proxying message to Complex890MessageHandler");
        return complex890MessageHandler.handleMessage(entity,paperRequest);
    }
}
