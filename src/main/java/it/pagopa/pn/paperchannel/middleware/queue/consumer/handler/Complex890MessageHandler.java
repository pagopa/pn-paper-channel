package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNAG012Wrapper;

import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;


/*
1. effettuare una query con operatore di uguaglianza su hashKey (META##<requestId>)
    Nota:
    i. Il risultato può contenere l’evento META##RECAG012
    ii. in caso di presenza del META##PNAG012 deve essere necessariamente presente un elemento META##RECAG012, in caso contrario segnalare il problema
    iii. Il risultato della query contiene sicuramente gli eventi META correlati allo specifico evento di fascicolo chiuso (a seconda del caso RECAG005A RECAG006A o RECAG007A)  - questi eventi sono ora da prendere in considerazione
    iv. evento META##RECAG011A

2. Nel caso in cui il risultato della query contenga l’entry META##PNAG012 allora dovrà essere inoltrato
    l’evento originale (RECAG005C RECAG006C o RECAG007C) con statusCode PROGRESS verso delivery_push

3. In caso contrario (l’entry META##PNAG012 non è presente), a questo punto va recuperato l’evento di
    pre-esito META##RECAG00_A (che può essere  RECAG005A RECAG006A o RECAG007A a seconda dei casi) e
    l’evento META##RECAG011A ed effettuata la differenza tra gli statusDateTime (META##RECAG00_A - META##RECAG011A)

    se (statusDateTime[META##RECAG00_A] - statusDateTime[META##RECAG011A]) < 10 giorni

        inoltro dell’evento originale (RECAG005C RECAG006C o RECAG007C) come finale verso deliveryPush

    se (statusDateTime[META##RECAG00_A] - statusDateTime[META##RECAG011A]) >= 10 giorni

        i. generare l’evento generazione dell’evento finale PNAG012 verso delivery_push (vedi dettaglio in paragrafo Evento PNAG012)  impostando come statusDateTime (META##RECAG011A+10 giorni) che dovrebbe sempre coincidere con META##RECAG012
        ii. inoltro dell’evento originale (RECAG005C RECAG006C o RECAG007C) come PROGRESS verso deliveryPush

4. A valle di questo processo vanno cancellate le righe in tabella per le hashKey DEMAT##<requestId> e META##<requestId>
*/

@Slf4j
public class Complex890MessageHandler extends SendToDeliveryPushHandler {

    private static final String META_RECAG012_STATUS_CODE = buildMetaStatusCode(RECAG012_STATUS_CODE);

    private static final String META_PNAG012_STATUS_CODE = buildMetaStatusCode(PNAG012_STATUS_CODE);

    private static final String META_RECAG011A_STATUS_CODE = buildMetaStatusCode(RECAG011A_STATUS_CODE);

    private final EventMetaDAO eventMetaDAO;

    private final MetaDematCleaner metaDematCleaner;

    private final Duration refinementDuration;

    public Complex890MessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, MetaDematCleaner metaDematCleaner, Duration refinementDuration) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
        this.metaDematCleaner = metaDematCleaner;

        if (refinementDuration == null)
            this.refinementDuration = Duration.of(10, ChronoUnit.DAYS);
        else
            this.refinementDuration = refinementDuration;

        log.info("Refinement duration is {}", this.refinementDuration);
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

        Instant recag011ADateTime = null;
        Instant recag00XADateTime = null;


        for (PnEventMeta pnEventMeta: pnEventMetas) {
            if (META_PNAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode())) {
                containsPNAG012 = true;
            }
            else if (META_RECAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode())) {
                containsRECAG012 = true;
            }
            else if (META_RECAG011A_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode())) {
                recag011ADateTime = pnEventMeta.getStatusDateTime();
            }
            else if (checkForMetaCorrespondence(paperRequest, pnEventMeta)) {
                recag00XADateTime = pnEventMeta.getStatusDateTime();
            }
        }


        if (containsPNAG012 && (!containsRECAG012)) {  // presente META##PNAG012 ma NON META##RECAG012
//            CASO 1.ii
            log.error("[{}] META##PNAG012 is present but META##RECAG012 is not present", paperRequest.getRequestId());
            return Mono.empty();
        }
        else if (containsPNAG012 /*&& containsRECAG012*/) { // presenti META##RECAG012  e META##PNAG012
//            CASO 2
            log.info("[{}] Result of query is: META##RECAG012 present, META##PNAG012 present", paperRequest.getRequestId());
            return super.handleMessage(SendEventMapper.changeToProgressStatus(entity), paperRequest)
                    .then(Mono.just(pnEventMetas));
        }
        else if (/*(!containsPNAG012) &&*/ containsRECAG012) { // presente META##RECAG012  e non META##PNAG012
//            CASO 3
            log.info("[{}] Result of query is: META##RECAG012 present, META##PNAG012 not present", paperRequest.getRequestId());

            if (missingRequiredDateTimes(recag011ADateTime, recag00XADateTime)) {
                log.error("[{}] needed META##RECAG00_A is present and/or META##RECAG011A not present", paperRequest.getRequestId());
                return Mono.empty();
            }

            if (lessThanTenDaysBetweenRECAG00XAAndRECAG011A(recag011ADateTime, recag00XADateTime)) {
                // 3 a
                log.info("[{}] (statusDateTime[META##RECAG00_A] - statusDateTime[META##RECAG011A]) < {}", paperRequest.getRequestId(), refinementDuration);

                return super.handleMessage(entity, paperRequest) // original event sent as final
                        .then(Mono.just(pnEventMetas));
            } else if (recag011ADateTime != null) { // if check only for not having a warning when calling .plus
                // 3 b
                log.info("[{}] (statusDateTime[META##RECAG00_A] - statusDateTime[META##RECAG011A]) >= {}", paperRequest.getRequestId(), refinementDuration);

                PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
                PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);
                PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, recag011ADateTime.plus(refinementDuration));
                var pnag012PaperRequest = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
                var pnag012DeliveryRequest = pnag012Wrapper.getPnDeliveryRequestPNAG012();

                pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel",pnag012DeliveryRequest.getRequestId()));
                logSuccessAuditLog(pnag012PaperRequest, pnag012DeliveryRequest, pnLogAudit);

                return super.handleMessage(pnag012DeliveryRequest, pnag012PaperRequest) // generated event sent as final
                        .then(super.handleMessage(SendEventMapper.changeToProgressStatus(entity), paperRequest))  // original event sent as progress
                        .then(Mono.just(pnEventMetas));
            }
        }
        // RECAG012 sarà sempre presente: lasciato quest'ultimo caso solo per esaurire le casistiche e fare comunque la return
//            CASO 4
        log.info("[{}] Result of query has no PNAG012 and no RECAG012", paperRequest.getRequestId());
        // qui lo status è OK o KO in base alla trasformazione fatta nel passo di update entity
        return super.handleMessage(entity, paperRequest)
                .then(Mono.just(pnEventMetas));
    }

    boolean checkForMetaCorrespondence(PaperProgressStatusEventDto paperRequest,  PnEventMeta pnEventMeta) {
        var metaPrefix = "META##";
        var paperRequestStatusCode = paperRequest.getStatusCode();
        var metaStatusCode = pnEventMeta.getMetaStatusCode().replace(metaPrefix, "");

        return paperRequestStatusCode.equals("RECAG005C") && metaStatusCode.equals("RECAG005A")
                || paperRequestStatusCode.equals("RECAG006C") && metaStatusCode.equals("RECAG006A")
                || paperRequestStatusCode.equals("RECAG007C") && metaStatusCode.equals("RECAG007A");
    }

    boolean lessThanTenDaysBetweenRECAG00XAAndRECAG011A(Instant recag011ADateTime, Instant recag00XADateTime) {
        // sebbene 10gg sia il termine di esercizio, per collaudo fa comodo avere un tempo più contenuto
        return Duration.between(recag011ADateTime, recag00XADateTime).compareTo(refinementDuration) < 0;
    }

    boolean missingRequiredDateTimes(Instant recag011ADateTime, Instant recag00XADateTime) {
        return (recag011ADateTime == null || recag00XADateTime == null);
    }
}
