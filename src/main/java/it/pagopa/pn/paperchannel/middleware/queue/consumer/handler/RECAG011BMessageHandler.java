package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildDocumentTypeStatusCode;

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


    protected static final String META_SORT_KEY_FILTER = buildMetaStatusCode(RECAG012_STATUS_CODE);

    private static final String DEMAT_23L_RECAG011B = buildDocumentTypeStatusCode("23L", RECAG011B_STATUS_CODE);

    private static final String DEMAT_ARCAD_RECAG011B = buildDocumentTypeStatusCode("ARCAD", RECAG011B_STATUS_CODE);

    private static final String DEMAT_CAD_RECAG011B = buildDocumentTypeStatusCode("CAD", RECAG011B_STATUS_CODE);

    protected static final String[] DEMAT_SORT_KEYS_FILTER = {
            DEMAT_23L_RECAG011B,
            DEMAT_ARCAD_RECAG011B,
            DEMAT_CAD_RECAG011B,
    };

    private final EventMetaDAO eventMetaDAO;

    private final Long ttlDaysMeta;

    public RECAG011BMessageHandler(SqsSender sqsSender, EventDematDAO eventDematDAO, Long ttlDaysDemat, EventMetaDAO eventMetaDAO, Long ttlDaysMeta) {
        super(sqsSender, eventDematDAO, ttlDaysDemat);
        this.eventMetaDAO = eventMetaDAO;
        this.ttlDaysMeta = ttlDaysMeta;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG011B handler start", paperRequest.getRequestId());

        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        String dematRequestId = buildDematRequestId(paperRequest.getRequestId());

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);
        return super.handleMessage(entity, paperRequest)
                .then(super.eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER).collectList())
                .filter(this::canCreatePNAG012Event)
                .doOnNext(pnEventDemats -> log.info("[{}] CanCreatePNAG012Event Filter success", paperRequest.getRequestId()))
                .flatMap(pnEventDemats ->  eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER ))
                .doOnNext(pnEventMeta -> log.info("[{}] PnEventMeta found: {}", paperRequest.getRequestId(), pnEventMeta))
                .map(pnEventMetaRECAG012 -> createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, ttlDaysMeta))
                .doOnNext(pnEventMeta -> pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel", entity.getRequestId())))
                .flatMap(eventMetaDAO::createOrUpdate)
                .doOnNext(pnEventMeta -> logSuccessAuditLog(paperRequest, entity, pnLogAudit))
                .doOnNext(pnEventMeta -> editPnDeliveryRequestForPNAG012(entity))
                .doOnNext(pnEventMeta -> log.info("[{}] Sending PNAG012 to delivery push: {}", paperRequest.getRequestId(), entity))
                .flatMap(pnEventMeta -> super.sendToDeliveryPush(entity, paperRequest));
    }

    private boolean canCreatePNAG012Event(List<PnEventDemat> pnEventDemats) {
        Optional<PnEventDemat> twentyThreeLElement = pnEventDemats.stream()
                .filter(pnEventDemat -> DEMAT_23L_RECAG011B.equals(pnEventDemat.getDocumentTypeStatusCode()))
                .findFirst();

        Optional<PnEventDemat> arcadOrCadElement = pnEventDemats.stream()
                .filter(pnEventDemat -> DEMAT_ARCAD_RECAG011B.equals(pnEventDemat.getDocumentTypeStatusCode()) ||
                        DEMAT_CAD_RECAG011B.equals(pnEventDemat.getDocumentTypeStatusCode()))
                .findFirst();

        return twentyThreeLElement.isPresent() && arcadOrCadElement.isPresent();
    }

    // simulo lo stesso log di evento ricevuto da ext-channels
    private void logSuccessAuditLog(PaperProgressStatusEventDto paperRequest, PnDeliveryRequest entity, PnLogAudit pnLogAudit) {
        paperRequest.setStatusCode(PNAG012_STATUS_CODE);
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto().analogMail(paperRequest);
        pnLogAudit.addsSuccessReceive(entity.getIun(),
                String.format("prepare requestId = %s Response %s from external-channel status code %s",
                        entity.getRequestId(), singleStatusUpdateDto.toString().replaceAll("\n", ""), entity.getStatusCode()));
    }

}
