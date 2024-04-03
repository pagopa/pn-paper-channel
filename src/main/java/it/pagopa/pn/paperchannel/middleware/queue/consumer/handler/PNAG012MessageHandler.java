package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNAG012Wrapper;
import it.pagopa.pn.paperchannel.utils.DematDocumentTypeEnum;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

/**
 * Questo handler è l'unico in cui viene scatenato da altri handler, {@link RECAG011BMessageHandler} e {@link RECAG012MessageHandler}
 * e non direttamente da un evento di ext-channel.
 */
// 1. Chiamare una batchGetItems utilizzando le seguenti chiavi:
//        23L##RECAG011B
//        ARCAD##RECAG011B
//        CAD##RECAG011B
// 2. Nel caso in cui risultano presenti il 23L##RECAG011B e uno degli altri due element effettuare la transizione in "Distacco d'ufficio 23L fascicolo chiuso":
//        1. Recuperare l’evento con SK META##RECAG012 e recuperare la statusDateTime (se non esiste il record, bloccare il flusso)
//        2. effettuare PUT_IF_ABSENT di una nuova riga correlata all’evento PNAG012 in tabella impostando come statusDateTime quella recuperata al punto precedente
//        3. inoltrare l’evento PNAG012 verso delivery_push (solo se la PUT_IF_ABSENT ha inserito il record)
@Slf4j
@SuperBuilder
public class PNAG012MessageHandler extends SaveDematMessageHandler {

    protected static final String META_SORT_KEY_FILTER = buildMetaStatusCode(RECAG012_STATUS_CODE);
    protected static final String DEMAT_23L_RECAG011B = buildDocumentTypeStatusCode(DematDocumentTypeEnum.DEMAT_23L.getDocumentType(), RECAG011B_STATUS_CODE);

    protected static final String DEMAT_ARCAD_RECAG011B = buildDocumentTypeStatusCode(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType(), RECAG011B_STATUS_CODE);

    protected static final String DEMAT_CAD_RECAG011B = buildDocumentTypeStatusCode(DematDocumentTypeEnum.DEMAT_CAD.getDocumentType(), RECAG011B_STATUS_CODE);

    protected static final String DEMAT_23L_RECAG008B = buildDocumentTypeStatusCode(DematDocumentTypeEnum.DEMAT_23L.getDocumentType(), RECAG008B_STATUS_CODE);

    protected static final String DEMAT_ARCAD_RECAG008B = buildDocumentTypeStatusCode(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType(), RECAG008B_STATUS_CODE);

    protected static final String DEMAT_CAD_RECAG008B = buildDocumentTypeStatusCode(DematDocumentTypeEnum.DEMAT_CAD.getDocumentType(), RECAG008B_STATUS_CODE);

    protected static final String[] DEMAT_SORT_KEYS_FILTER = {
            DEMAT_23L_RECAG011B,
            DEMAT_ARCAD_RECAG011B,
            DEMAT_CAD_RECAG011B,
            DEMAT_23L_RECAG008B,
            DEMAT_ARCAD_RECAG008B,
            DEMAT_CAD_RECAG008B
    };

    private final EventMetaDAO eventMetaDAO;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        String dematRequestId = buildDematRequestId(paperRequest.getRequestId());

        PnLogAudit pnLogAudit = new PnLogAudit();

        return super.eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER).collectList()
                .doOnNext(pnEventDemats -> log.debug("Result of findAllByKeys: {}", pnEventDemats))
                .filter(this::canCreatePNAG012Event)
                .doOnDiscard(List.class, o -> log.info("PNAG012 filter not passed"))
                .doOnNext(pnEventDemats -> log.info("[{}] CanCreatePNAG012Event Filter success", paperRequest.getRequestId()))
                .flatMap(pnEventDemats ->
                        eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER)
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.warn("[{}] Meta with RECAG012 not found", paperRequest.getRequestId());
                                    return Mono.empty();
                                }))
                )
                .doOnNext(pnEventMeta -> log.info("[{}] PnEventMeta found: {}", paperRequest.getRequestId(), pnEventMeta))
                .map(pnEventMetaRECAG012 -> createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, pnPaperChannelConfig.getTtlExecutionDaysMeta()))
                .flatMap(pnEventMetaPNAG012 -> this.pnag012Flow(pnEventMetaPNAG012, entity, paperRequest, pnLogAudit))
                .doOnNext(pnag012Wrapper -> log.info("[{}] Sending PNAG012 to delivery push: {}", pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012().getRequestId(), pnag012Wrapper.getPnDeliveryRequestPNAG012()))
                .flatMap(pnag012Wrapper -> super.sendToDeliveryPush(pnag012Wrapper.getPnDeliveryRequestPNAG012(), pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012()));
    }


    /**
     * This method evaluates whether it is possible to create and send a PNAG012 event
     * checking if all required demats are included as subset of pnEventDemats.
     *
     * @param pnEventDematsFromDB the demats to check
     * @return              true if all required demats are included, false otherwise
     * */
    private boolean canCreatePNAG012Event(List<PnEventDemat> pnEventDematsFromDB) {

        Set<String> documentTypes = getDocumentTypesFromPnEventDemats(pnEventDematsFromDB);

        return documentTypes.containsAll(pnPaperChannelConfig.getRequiredDemats());
    }

    private Mono<PNAG012Wrapper> pnag012Flow(PnEventMeta pnEventMetaPNAG012, PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest, PnLogAudit pnLogAudit) {
        PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, pnEventMetaPNAG012.getStatusDateTime());
        var pnag012PaperRequest = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
        var pnag012DeliveryRequest = pnag012Wrapper.getPnDeliveryRequestPNAG012();
        pnLogAudit.addsBeforeReceive(pnag012DeliveryRequest.getIun(), String.format("prepare requestId = %s Response from external-channel", pnag012DeliveryRequest.getRequestId()));
        return eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)
                .doOnNext(pnEventMetaRECAG012Updated -> logSuccessAuditLog(pnag012PaperRequest, pnag012DeliveryRequest, pnLogAudit))
                .map(pnEventMeta -> pnag012Wrapper)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[{}] PNAG012 already exist", paperRequest.getRequestId());
                    return Mono.empty();
                })); //blocco il flusso se la putIfAbsent non inserisce a DB
    }

    private Set<String> getDocumentTypesFromPnEventDemats(List<PnEventDemat> pnEventDematsFromDB) {
        return pnEventDematsFromDB.stream()
                .filter(pnEventDemat -> pnEventDemat.getDocumentType() != null)
                .map(pnEventDemat -> DematDocumentTypeEnum.getAliasFromDocumentType(pnEventDemat.getDocumentType()))
                .collect(Collectors.toSet());
    }
}
