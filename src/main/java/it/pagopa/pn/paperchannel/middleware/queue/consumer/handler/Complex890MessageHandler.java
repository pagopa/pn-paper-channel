package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNAG012Wrapper;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
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
   - generazione dell’evento finale PNAG012 verso delivery_push (vedi dettaglio in paragrafo Evento PNAG012)
   - inoltro dell’evento originale (RECAG005C RECAG006C o RECAG007C) con statusCode PROGRESS verso delivery_push

4. Nel caso in cui il risultato della query non produca nessun risultato (raggiunto stato finale di recapito entro il
        perfezionamento d’ufficio) allora lo stato originale (RECAG005C RECAG006C o RECAG007C)  potrà essere considerato FINALE ed inoltrato direttamente a delivery_push

5. A valle di questo processo vanno cancellate le righe in tabella per le hashKey DEMAT##<requestId> e META##<requestId>
*/
@Slf4j
public class Complex890MessageHandler extends SendToDeliveryPushHandler {

    private static final String META_RECAG012_STATUS_CODE = buildMetaStatusCode(RECAG012_STATUS_CODE);

    private static final String META_PNAG012_STATUS_CODE = buildMetaStatusCode(PNAG012_STATUS_CODE);

    private final EventMetaDAO eventMetaDAO;

    private final MetaDematCleaner metaDematCleaner;

    public Complex890MessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, MetaDematCleaner metaDematCleaner) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
        this.metaDematCleaner = metaDematCleaner;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String pkMetaFilter = buildMetaRequestId(paperRequest.getRequestId());
        return eventMetaDAO.findAllByRequestId(pkMetaFilter)
                .collectList()
                .doOnNext(pnEventMetas -> log.info("[{}] Result of findAllByRequestId: {}", paperRequest.getRequestId(), pnEventMetas))
                .flatMap(pnEventMetas -> handleMetasResult(pnEventMetas, entity, paperRequest))
                .then(metaDematCleaner.clean(paperRequest.getRequestId()));
    }

    private Mono<List<PnEventMeta>> handleMetasResult(List<PnEventMeta> pnEventMetas, PnDeliveryRequest entity,
                                                      PaperProgressStatusEventDto paperRequest) {
        boolean containsPNAG012 = false;
        boolean containsRECAG012 = false;
        PnEventMeta pnEventMetaRECAG012 = null;


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
            log.error("[{}] META##PNAG012 is present but META##RECAG012 is not present", paperRequest.getRequestId());
            return Mono.empty();
        }
        else if (containsPNAG012 && containsRECAG012) { // presenti META##RECAG012  e META##PNAG012
//            CASO 2
            log.info("[{}] Result of query is: META##RECAG012 present, META##PNAG012 present", paperRequest.getRequestId());
            entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());
            return super.handleMessage(entity, paperRequest)
                    .then(Mono.just(pnEventMetas));
        }
        else if ( (!containsPNAG012) && containsRECAG012) { // presente META##RECAG012  e non META##PNAG012
//            CASO 3
            log.info("[{}] Result of query is: META##RECAG012 present, META##PNAG012 not present", paperRequest.getRequestId());
            PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
            PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);
            PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, pnEventMetaRECAG012.getStatusDateTime());
            var pnag012PaperRequest = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
            var pnag012DeliveryRequest = pnag012Wrapper.getPnDeliveryRequestPNAG012();
            pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel",pnag012DeliveryRequest.getRequestId()));
            logSuccessAuditLog(pnag012PaperRequest, pnag012DeliveryRequest, pnLogAudit);

            entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());
            return super.handleMessage(pnag012DeliveryRequest, pnag012PaperRequest)
                    .then(super.handleMessage(entity, paperRequest))
                    .then(Mono.just(pnEventMetas));
        }

        else { // entrambi non presenti
//            CASO 4
            log.info("[{}] Result of query has no PNAG012 and no RECAG012", paperRequest.getRequestId());
            // qui lo status è OK o KO in base alla trasformazione fatta nel passo di update entity
            return super.handleMessage(entity, paperRequest)
                    .then(Mono.just(pnEventMetas));
        }

    }

}
