package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// handler salva metadati (vedere Gestione eventi di pre-esito)
@RequiredArgsConstructor
@Slf4j
public class SaveMetadataMessageHandler implements MessageHandler {

    private static final String METADATA_PREFIX = "META";

    private static final String METADATA_DELIMITER = "##";

    private final EventMetaDAO eventMetaDAO;

    private final Long ttlDays;


    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] Saving PaperRequest from ExcChannel", paperRequest.getRequestId());
        PnEventMeta pnEventMeta = buildPnEventMeta(paperRequest);
        eventMetaDAO.createOrUpdate(pnEventMeta)
                        .doOnNext(savedEntity ->  log.info("[{}] Saved PaperRequest from ExcChannel: {}", paperRequest.getRequestId(), savedEntity))
                        .block();
    }

    protected PnEventMeta buildPnEventMeta(PaperProgressStatusEventDto paperRequest) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(METADATA_PREFIX + METADATA_DELIMITER + paperRequest.getRequestId());
        pnEventMeta.setMetaStatusCode(METADATA_PREFIX + METADATA_DELIMITER + paperRequest.getStatusCode());
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDays).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(paperRequest.getStatusCode());
        pnEventMeta.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());
        pnEventMeta.setDiscoveredAddress(pnEventMeta.getDiscoveredAddress());
        pnEventMeta.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        return pnEventMeta;
    }

}
