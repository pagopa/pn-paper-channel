package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
public class RECAG011BMessageHandler extends SaveDematMessageHandler {

    private static final String[] SORT_KEYS_FILTER = { "23L##RECAG011B",  "ARCAD##RECAG011B", "CAD##RECAG011B" };

    private final EventMetaDAO eventMetaDAO;

    public RECAG011BMessageHandler(SqsSender sqsSender, EventDematDAO eventDematDAO, Long ttlDays, EventMetaDAO eventMetaDAO) {
        super(sqsSender, eventDematDAO, ttlDays);
        this.eventMetaDAO = eventMetaDAO;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String dematRequestId = DEMAT_PREFIX + DEMAT_DELIMITER + paperRequest.getRequestId();
        return super.handleMessage(entity, paperRequest)
                .flatMap(unused -> super.eventDematDAO.findAllByKeys(dematRequestId, SORT_KEYS_FILTER).collectList())
                .filter(this::canCreatePNAG012Event)
                .map(pnEventDemats -> createPNAG012Event(paperRequest))
                .flatMap(eventMetaDAO::createOrUpdate)
                .flatMap(pnEventMeta -> super.sendToDeliveryPush(entity, paperRequest))
                .then();

    }

    private boolean canCreatePNAG012Event(List<PnEventDemat> pnEventDemats) {
        Optional<PnEventDemat> twentyThreeLElement = pnEventDemats.stream()
                .filter(pnEventDemat -> "23L##RECAG011B".equals(pnEventDemat.getDocumentType()))
                .findFirst();

        Optional<PnEventDemat> arcadOrCadElement = pnEventDemats.stream()
                .filter(pnEventDemat -> "ARCAD##RECAG011B".equals(pnEventDemat.getDocumentType()) || "CAD##RECAG011B".equals(pnEventDemat.getDocumentType()))
                .findFirst();

        return twentyThreeLElement.isPresent() && arcadOrCadElement.isPresent();
    }

    private PnEventMeta createPNAG012Event(PaperProgressStatusEventDto paperRequest) {
        return new PnEventMeta();
    }

}
