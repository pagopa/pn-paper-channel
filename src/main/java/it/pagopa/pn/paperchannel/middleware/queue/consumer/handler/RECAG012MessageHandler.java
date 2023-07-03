package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

// Il RECAG012 è considerato come entità logica un metadata
// Viene cercato sulla tabella META se esiste già l'evento, se non esiste in tabella, viene salvato a DB,
// altrimenti l'evento arrivato viene ignorato
@Slf4j
public class RECAG012MessageHandler extends SaveMetadataMessageHandler {


    public RECAG012MessageHandler(EventMetaDAO eventMetaDAO, Long ttlDays) {
        super(eventMetaDAO, ttlDays);
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG012 handler start", paperRequest.getRequestId());
        String partitionKey =  buildMetaRequestId(paperRequest.getRequestId());
        String sortKey = buildMetaStatusCode(paperRequest.getStatusCode());
        return super.eventMetaDAO.getDeliveryEventMeta(partitionKey, sortKey)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(optionalPnEventMeta -> saveIfNotExistsInDB(optionalPnEventMeta, entity, paperRequest));
    }

    private Mono<Void> saveIfNotExistsInDB(Optional<PnEventMeta> optionalPnEventMeta, PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        if(optionalPnEventMeta.isPresent()) {
            PnEventMeta pnEventMetaInDB = optionalPnEventMeta.get();
            PnEventMeta pnEventMetaNew = super.buildPnEventMeta(paperRequest);

            // se mi arriva uno stesso evento (requestId, statusCode) più di una volta, ma con gli stessi campi,
            // semplicemente lo ignoro. Ma se arriva con valori diversi, loggo un error
            if(! pnEventMetaInDB.equals(pnEventMetaNew)) {
                log.error("[{}] Entity RECAG012 already present. In DB: {}, received: {}", paperRequest.getRequestId(), pnEventMetaInDB, pnEventMetaNew);
            }
            return Mono.empty();
        }
        else {
            // se non è già presente a DB, lo salvo come entità META
            return super.handleMessage(entity, paperRequest);
        }
    }

}
