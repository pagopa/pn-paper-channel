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
// Questo handler per ora è pensato per essere poi eseguito in catena con il flusso PNAG012 semplificato, al netto del superamento delle varie condizioni che si trovano nell'handler del PNAG012.
// L'aggiunta della gestion del PNAG012 è stata fatta a seguito del task PN-8911.
@Slf4j
@SuperBuilder
public class RECAG012MessageHandler extends SaveMetadataMessageHandler {

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG012 handler start", paperRequest.getRequestId());
        String partitionKey =  buildMetaRequestId(paperRequest.getRequestId());
        String sortKey = buildMetaStatusCode(paperRequest.getStatusCode());

        return super.eventMetaDAO.getDeliveryEventMeta(partitionKey, sortKey)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(optionalPnEventMeta -> saveIfNotExistsInDB(optionalPnEventMeta, entity, paperRequest))
                .doOnNext(deliveryRequest -> log.debug("[{}] RECAG012 handler ended", paperRequest.getRequestId()))
                .doOnError(ex -> log.warn("[{}] RECAG012 handler ended with error: {}", paperRequest.getRequestId(), ex.getMessage()))
                .then();
    }

    /**
     *
     * @param optionalPnEventMeta
     * @param entity
     * @param paperRequest
     * @return Restituisce l'entità PnDeliveryRequest dato in input, se inserisce il record pnEventMeta in DB (e quindi riceve per
     * la prima volta l'evento RECAG012 oppure se l'evento che riceve è già presente in DB, con gli stessi campi valorizzati.
     * Se invece l'evento che riceve è già presente in DB ma con campi valorizzati diversamente, viene lanciata una eccezione.
     */
    protected Mono<PnDeliveryRequest> saveIfNotExistsInDB(Optional<PnEventMeta> optionalPnEventMeta, PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        if(optionalPnEventMeta.isPresent()) {
            PnEventMeta pnEventMetaInDB = optionalPnEventMeta.get();
            PnEventMeta pnEventMetaNew = super.buildPnEventMeta(paperRequest);

            // se mi arriva uno stesso evento (requestId, statusCode) più di una volta, ma con gli stessi campi,
            // semplicemente lo ignoro. Ma se arriva con valori diversi, lancio l'eccezione
            if(! pnEventMetaInDB.equals(pnEventMetaNew)) {
                throw new PnGenericException(WRONG_RECAG012_DATA, "[{" + paperRequest.getRequestId() + "}] Entity RECAG012 already present. In DB: {" + pnEventMetaInDB + "}, received: {" + pnEventMetaNew + "}");
            }
            return Mono.just(entity);
        }
        else {
            // se non è già presente a DB, lo salvo come entità META
            return super.handleMessage(entity, paperRequest).thenReturn(entity);
        }
    }

}
