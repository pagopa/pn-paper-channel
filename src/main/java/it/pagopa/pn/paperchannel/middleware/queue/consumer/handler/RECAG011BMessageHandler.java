package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

// 1. effettuare PUT di una riga per ogni documento dematerializzato allegato in accordo con le specifiche
// 2. invocare l'handler di PNAG012

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

        return super.handleMessage(entity, paperRequest).thenReturn(entity)
                .doOnNext(deliveryRequest -> log.info("[{}] Start PNAG012MessageHandler from RECAG011B flow", paperRequest.getRequestId()))
                .then(pnag012MessageHandler.handleMessage(entity, paperRequest).thenReturn(entity))
                .doOnNext(deliveryRequest -> log.debug("[{}] RECAG011B handler ended", paperRequest.getRequestId()))
                .doOnError(ex -> log.warn("[{}] RECAG011B handler ended with error: {}", paperRequest.getRequestId(), ex.getMessage()))
                .then();
    }

}
