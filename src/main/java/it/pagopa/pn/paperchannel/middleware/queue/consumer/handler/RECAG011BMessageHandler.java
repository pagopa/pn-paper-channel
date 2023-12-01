package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

// 1. effettuare PUT di una riga per ogni documento dematerializzato allegato in accordo con le specifiche
// 2. invocare contestualmente alla PUT una batchGetItems utilizzando le seguenti chiavi:
//        23L##RECAG011B
//        ARCAD##RECAG011B
//        CAD##RECAG011B
// 3. Nel caso in cui risultano presenti il 23L##RECAG011B e uno degli altri due element effettuare la transizione in "Distacco d'ufficio 23L fascicolo chiuso":
//        1. Recuperare l’evento con SK META##RECAG012 e recuperare la statusDateTime
//        2. effettuare PUT di una nuova riga correlata all’evento PNAG012 in tabella impostando come statusDateTime quella recuperata al punto precedente
//        3. inoltrare l’evento PNAG012 verso delivery_push

@Slf4j
public class RECAG011BMessageHandler extends SaveDematMessageHandler {

    private final PNAG012MessageHandler pnag012MessageHandler;

    public RECAG011BMessageHandler(SqsSender sqsSender, EventDematDAO eventDematDAO, Long ttlDaysDemat, PNAG012MessageHandler pnag012MessageHandler) {
        super(sqsSender, eventDematDAO, ttlDaysDemat);
        this.pnag012MessageHandler = pnag012MessageHandler;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG011B handler start", paperRequest.getRequestId());

        return super.handleMessage(entity, paperRequest)
                .then(pnag012MessageHandler.handleMessage(entity, paperRequest));
    }

}
