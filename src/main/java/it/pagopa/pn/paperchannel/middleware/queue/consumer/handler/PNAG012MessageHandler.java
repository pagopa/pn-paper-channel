package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNAG012Wrapper;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.WRONG_EVENT_ORDER;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

@Slf4j
public class PNAG012MessageHandler extends SaveDematMessageHandler {

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

    public PNAG012MessageHandler(SqsSender sqsSender, EventDematDAO eventDematDAO, Long ttlDaysDemat, EventMetaDAO eventMetaDAO, Long ttlDaysMeta) {
        super(sqsSender, eventDematDAO, ttlDaysDemat);
        this.eventMetaDAO = eventMetaDAO;
        this.ttlDaysMeta = ttlDaysMeta;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        String dematRequestId = buildDematRequestId(paperRequest.getRequestId());

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);

        return super.eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER).collectList()
                .filter(this::canCreatePNAG012Event)
                .doOnNext(pnEventDemats -> log.info("[{}] CanCreatePNAG012Event Filter success", paperRequest.getRequestId()))
                .flatMap(pnEventDemats -> eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER)
                        .switchIfEmpty(Mono.error(new PnGenericException(WRONG_EVENT_ORDER, "[{" + paperRequest.getRequestId() + "}] Missing EventMeta for {" + paperRequest + "}")))
                )
                .doOnNext(pnEventMeta -> log.info("[{}] PnEventMeta found: {}", paperRequest.getRequestId(), pnEventMeta))
                .map(pnEventMetaRECAG012 -> createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, ttlDaysMeta))
                .flatMap(pnEventMetaPNAG012 -> this.pnag012Flow(pnEventMetaPNAG012, entity, paperRequest, pnLogAudit))
                .doOnNext(pnag012Wrapper -> log.info("[{}] Sending PNAG012 to delivery push: {}", pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012().getRequestId(), pnag012Wrapper.getPnDeliveryRequestPNAG012()))
                .flatMap(pnag012Wrapper -> super.sendToDeliveryPush(pnag012Wrapper.getPnDeliveryRequestPNAG012(), pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012()));
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

    private Mono<PNAG012Wrapper> pnag012Flow(PnEventMeta pnEventMetaPNAG012, PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest, PnLogAudit pnLogAudit) {
        PNAG012Wrapper pnag012Wrapper = PNAG012Wrapper.buildPNAG012Wrapper(entity, paperRequest, pnEventMetaPNAG012.getStatusDateTime());
        var pnag012PaperRequest = pnag012Wrapper.getPaperProgressStatusEventDtoPNAG012();
        var pnag012DeliveryRequest = pnag012Wrapper.getPnDeliveryRequestPNAG012();
        pnLogAudit.addsBeforeReceive(pnag012DeliveryRequest.getIun(), String.format("prepare requestId = %s Response from external-channel", pnag012DeliveryRequest.getRequestId()));
        return eventMetaDAO.createOrUpdate(pnEventMetaPNAG012)
                .doOnNext(pnEventMetaRECAG012Updated -> logSuccessAuditLog(pnag012PaperRequest, pnag012DeliveryRequest, pnLogAudit))
                .thenReturn(pnag012Wrapper);
    }
}
