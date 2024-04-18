package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;


import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.WRONG_RECAG012_DATA;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaRequestId;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaStatusCode;

// Il RECAG012 è considerato come entità logica un metadata
// Viene cercato sulla tabella META se esiste già l'evento, se non esiste in tabella, viene salvato a DB,
// altrimenti l'evento arrivato viene ignorato.
// Poi, esegue il flusso PNAG012, al netto del superamento delle varie condizioni che si trovano nell'handler del PNAG012.
// L'aggiunta della gestion del PNAG012 è stata fatta a seguito del task PN-8911.
@Slf4j
@SuperBuilder
public class OldRECAG012MessageHandler extends RECAG012MessageHandler {

    private final PNAG012MessageHandler pnag012MessageHandler;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG012 handler start", paperRequest.getRequestId());
        String partitionKey =  buildMetaRequestId(paperRequest.getRequestId());
        String sortKey = buildMetaStatusCode(paperRequest.getStatusCode());

        return super.eventMetaDAO.getDeliveryEventMeta(partitionKey, sortKey)
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .flatMap(optionalPnEventMeta -> super.saveIfNotExistsInDB(optionalPnEventMeta, entity, paperRequest))
            .flatMap(deliveryRequest -> pnag012MessageHandler.handleMessage(entity, paperRequest).thenReturn(entity))
            .doOnNext(deliveryRequest -> log.debug("[{}] RECAG012 handler ended", paperRequest.getRequestId()))
            .doOnError(ex -> log.warn("[{}] RECAG012 handler ended with error: {}", paperRequest.getRequestId(), ex.getMessage()))
            .then();
    }
}
