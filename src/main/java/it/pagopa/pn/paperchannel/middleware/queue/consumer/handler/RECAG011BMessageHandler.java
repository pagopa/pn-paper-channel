package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
public class RECAG011BMessageHandler extends SaveDematMessageHandler {

    private static final String META_PREFIX = "META";

    private static final String META_DELIMITER = "##";

    private static final String PNAG012_STATUS_CODE = "PNAG012";

    protected static final String META_SORT_KEY_FILTER = "META##RECAG012";

    protected static final String[] DEMAT_SORT_KEYS_FILTER = { "23L##RECAG011B", "ARCAD##RECAG011B", "CAD##RECAG011B" };

    private final EventMetaDAO eventMetaDAO;

    private final Long ttlDaysMeta;

    public RECAG011BMessageHandler(SqsSender sqsSender, EventDematDAO eventDematDAO, Long ttlDaysDemat, EventMetaDAO eventMetaDAO, Long ttlDaysMeta) {
        super(sqsSender, eventDematDAO, ttlDaysDemat);
        this.eventMetaDAO = eventMetaDAO;
        this.ttlDaysMeta = ttlDaysMeta;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String metadataRequestIdFilter = META_PREFIX + META_DELIMITER + paperRequest.getRequestId();
        String dematRequestId = DEMAT_PREFIX + DEMAT_DELIMITER + paperRequest.getRequestId();

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);
        return super.handleMessage(entity, paperRequest)
                .then(super.eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER).collectList())
                .filter(this::canCreatePNAG012Event)
                .flatMap(pnEventDemats ->  eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER ))
                .map(pnEventMetaRECAG012 -> createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012))
                .doOnNext(pnEventMeta -> pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel", entity.getRequestId())))
                .flatMap(eventMetaDAO::createOrUpdate)
                .doOnNext(pnEventMeta -> logSuccessAuditLog(paperRequest, entity, pnLogAudit))
                .doOnNext(pnEventMeta -> editPnDeliveryRequestForPNAG012(entity))
                .flatMap(pnEventMeta -> super.sendToDeliveryPush(entity, paperRequest));
    }

    private boolean canCreatePNAG012Event(List<PnEventDemat> pnEventDemats) {
        Optional<PnEventDemat> twentyThreeLElement = pnEventDemats.stream()
                .filter(pnEventDemat -> "23L##RECAG011B".equals(pnEventDemat.getDocumentTypeStatusCode()))
                .findFirst();

        Optional<PnEventDemat> arcadOrCadElement = pnEventDemats.stream()
                .filter(pnEventDemat -> "ARCAD##RECAG011B".equals(pnEventDemat.getDocumentTypeStatusCode()) || "CAD##RECAG011B".equals(pnEventDemat.getDocumentTypeStatusCode()))
                .findFirst();

        return twentyThreeLElement.isPresent() && arcadOrCadElement.isPresent();
    }

    protected PnEventMeta createMETAForPNAG012Event(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMetaRECAG012) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(META_PREFIX + META_DELIMITER + paperRequest.getRequestId());
        pnEventMeta.setMetaStatusCode(META_PREFIX + META_DELIMITER + PNAG012_STATUS_CODE);
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDaysMeta).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(PNAG012_STATUS_CODE);
        pnEventMeta.setStatusDateTime(pnEventMetaRECAG012.getStatusDateTime());
        return pnEventMeta;
    }

    // simulo lo stesso log di evento ricevuto da ext-channels
    private void logSuccessAuditLog(PaperProgressStatusEventDto paperRequest, PnDeliveryRequest entity, PnLogAudit pnLogAudit) {
        paperRequest.setStatusCode("RECAG012");
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto().analogMail(paperRequest);
        pnLogAudit.addsSuccessReceive(entity.getIun(),
                String.format("prepare requestId = %s Response %s from external-channel status code %s",
                        entity.getRequestId(), singleStatusUpdateDto.toString().replaceAll("\n", ""), entity.getStatusCode()));
    }

    protected void editPnDeliveryRequestForPNAG012(PnDeliveryRequest entity) {
        entity.setStatusCode(StatusCodeEnum.OK.getValue()); //TODO in attesa di chiarimento, se è OK o KO
        entity.setStatusDetail("Distacco d'ufficio 23L fascicolo chiuso");
    }

}
