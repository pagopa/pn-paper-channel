package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

/*
RECAG005C RECAG006C o RECAG007C

1. effettuare una query con operatore di uguaglianza su hashKey (META##<requestId>)
   Nota:
     i. Il risultato può contenere l’evento META##RECAG012
     ii. in caso di presenza del META##PNAG012 deve essere necessariamente presente un elemento META##RECAG012, in caso contrario segnalare il problema

2. Nel caso in cui il risultato della query produca le entry META##RECAG012  e META##PNAG012 allora dovrà essere inoltrato
        l’evento orginale (RECAG005C RECAG006C o RECAG007C) con statusCode PROGRESS verso delivery_push

3. Nel caso in cui il risultato della query produca il risultato META##RECAG012  senza il META##PNAG012 allora dovrà
        essere effettuata la transizione in "Distacco d'ufficio 23L fascicolo chiuso":
   - Recuperare l’evento con SK META##RECAG012 e recuperare la statusDateTime
     effettuare PUT di una nuova riga correlata all’evento PNAG012 in tabella impostando come statusDateTime quella recuperata al punto precedente
   - generazione dell’evento finale PNAG012 verso delivery_push (vedi dettaglio in paragrafo Evento PNAG012)
   - inoltro dell’evento orginale (RECAG005C RECAG006C o RECAG007C) con statusCode PROGRESS verso delivery_push

4. Nel caso in cui il risultato della query non produca nessun risultato (raggiunto stato finale di recapito entro il
        perfezionamento d’ufficio) allora lo stato originale (RECAG005C RECAG006C o RECAG007C)  potrà essere considerato FINALE ed inoltrato direttamente a delivery_push

5. A valle di questo processo vanno cancellate le righe in tabella per le hashKey DEMAT##<requestId> e META##<requestId>
*/
@Slf4j
public class Complex890MessageHandler extends SendToDeliveryPushHandler {

    private static final String META_RECAG012_STATUS_CODE = buildMetaStatusCode(RECAG012_STATUS_CODE);

    private static final String META_PNAG012_STATUS_CODE = buildMetaStatusCode(PNAG012_STATUS_CODE);

    private final EventMetaDAO eventMetaDAO;

    private final EventDematDAO eventDematDAO;

    private final Long ttlDaysMeta;

    public Complex890MessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, EventDematDAO eventDematDAO, Long ttlDaysMeta) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
        this.eventDematDAO = eventDematDAO;
        this.ttlDaysMeta = ttlDaysMeta;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String pkMetaFilter = buildMetaRequestId(paperRequest.getRequestId());
        String pkDematFilter = buildDematRequestId(paperRequest.getRequestId());
        return eventMetaDAO.findAllByRequestId(pkMetaFilter)
                .collectList()
                .doOnNext(pnEventMetas -> log.info("[{}] Result of findAllByRequestId: {}", paperRequest.getRequestId(), pnEventMetas))
                .flatMap(pnEventMetas -> handleMetasResult(pnEventMetas, entity, paperRequest))
                .map(this::mapMetasToSortKeys)
                .flatMap(sortKeysMeta-> eventMetaDAO.deleteBatch(pkMetaFilter, sortKeysMeta))
                .then(eventDematDAO.findAllByRequestId(pkDematFilter).collectList())
                .map(this::mapDematsToSortKeys)
                .flatMap(sortKeysDemat -> eventDematDAO.deleteBatch(pkDematFilter, sortKeysDemat));
    }

    private Mono<List<PnEventMeta>> handleMetasResult(List<PnEventMeta> pnEventMetas, PnDeliveryRequest entity,
                                                      PaperProgressStatusEventDto paperRequest) {
        boolean containsPNAG012 = false;
        boolean containsRECAG012 = false;
        PnEventMeta pnEventMetaRECAG012 = null;

        if(CollectionUtils.isEmpty(pnEventMetas)) {
//            CASO 4
            entity.setStatusCode(StatusCodeEnum.OK.getValue());
            return super.handleMessage(entity, paperRequest)
                    .then(Mono.just(pnEventMetas));
        }

        for (PnEventMeta pnEventMeta: pnEventMetas) {
            if(META_PNAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode())) {
                containsPNAG012 = true;
            }
            if(META_RECAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode())) {
                containsRECAG012 = true;
                pnEventMetaRECAG012 = pnEventMeta;
            }
        }


        if (containsPNAG012 && (!containsRECAG012)) {  // presente META##PNAG012 ma NON META##RECAG012
//            CASO 1.ii
            throw new RuntimeException();
        }
        else if (containsPNAG012 && containsRECAG012) { // presenti META##RECAG012  e META##PNAG012
//            CASO 2
            entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());
            return super.handleMessage(entity, paperRequest)
                    .then(Mono.just(pnEventMetas));
        }
        else if ( (!containsPNAG012) && containsRECAG012) { // presente META##RECAG012  e non META##PNAG012
//            CASO 3
            PnEventMeta metaForPNAG012Event = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, ttlDaysMeta);
            return eventMetaDAO.createOrUpdate(metaForPNAG012Event)
                    .doOnNext(pnEventMeta -> editPnDeliveryRequestForPNAG012(entity))
                    .flatMap(pnEventMeta -> super.handleMessage(entity, paperRequest))
                    .then(Mono.just(pnEventMetas));
        }

        return Mono.just(pnEventMetas);
    }

    private String[] mapMetasToSortKeys(List<PnEventMeta> pnEventMetas) {
        return pnEventMetas.stream().map(PnEventMeta::getMetaStatusCode).toArray(String[]::new);
    }

    private String[] mapDematsToSortKeys(List<PnEventDemat> pnEventDemats) {
        return pnEventDemats.stream().map(PnEventDemat::getDocumentTypeStatusCode).toArray(String[]::new);
    }

}
