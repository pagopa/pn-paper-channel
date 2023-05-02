package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

//effettuare PUT di una nuova riga in accordo con le specifiche
//in caso di RECAG012 la document date coincide con la data di produzione dell’evento
//In caso di ricezione di RECAG012 il documentType non sarà presente, il campo DOCUMENT_TYPE  va quindi popolato con una stringa fissa
@Slf4j
public class RECAG012MessageHandler implements MessageHandler {

    private static final String PARTITION_KEY_PREFIX = "DEMAT";
    private static final String SORT_KEY_PREFIX = "RECAG012";

    private static final String RECAG012_DELIMITER = "##";

    private final EventMetaDAO eventMetaDAO;

    private final Long ttlDays;

    public RECAG012MessageHandler(EventMetaDAO eventMetaDAO, Long ttlDays) {
        this.eventMetaDAO = eventMetaDAO;
        this.ttlDays = ttlDays;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] Saving PaperRequest from ExcChannel", paperRequest.getRequestId());
        PnEventMeta pnEventMeta = buildPnEventMeta(paperRequest);
        return eventMetaDAO.createOrUpdate(pnEventMeta)
                .doOnNext(savedEntity ->  log.info("[{}] Saved PaperRequest from ExcChannel: {}", paperRequest.getRequestId(), savedEntity))
                .then();
    }

    protected PnEventMeta buildPnEventMeta(PaperProgressStatusEventDto paperRequest) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(PARTITION_KEY_PREFIX + RECAG012_DELIMITER + paperRequest.getRequestId());
        pnEventMeta.setMetaStatusCode(SORT_KEY_PREFIX + RECAG012_DELIMITER + paperRequest.getStatusCode());
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDays).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(paperRequest.getStatusCode());
        pnEventMeta.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());
        pnEventMeta.setDiscoveredAddress(pnEventMeta.getDiscoveredAddress());
        pnEventMeta.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        return pnEventMeta;
    }

}
