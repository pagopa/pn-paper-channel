package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

//effettuare PUT di una nuova riga in accordo con le specifiche
// Il RECAG012 è considerato come entità logica un metadata
@Slf4j
public class RECAG012MessageHandler extends SaveMetadataMessageHandler {


    public RECAG012MessageHandler(EventMetaDAO eventMetaDAO, Long ttlDays) {
        super(eventMetaDAO, ttlDays);
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG012 handler start", paperRequest.getRequestId());
        return super.handleMessage(entity, paperRequest);
    }

}
