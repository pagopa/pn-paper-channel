package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.RequiredArgsConstructor;

// handler salva metadati (vedere Gestione eventi di pre-esito)
@RequiredArgsConstructor
public class SaveMetadataMessageHandler implements MessageHandler {

    private static final String METADATA_PREFIX = "META";

    private final EventMetaDAO eventMetaDAO;

    private final Long ttlDays;


    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        PnEventMeta pnEventMeta = buildPnEventMeta(paperRequest);
        eventMetaDAO.createOrUpdate(pnEventMeta).subscribe();
    }

    protected PnEventMeta buildPnEventMeta(PaperProgressStatusEventDto paperRequest) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(METADATA_PREFIX + paperRequest.getRequestId());
        pnEventMeta.setMetaStatusCode(METADATA_PREFIX + paperRequest.getStatusCode());
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDays).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(paperRequest.getStatusCode());
        pnEventMeta.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());
        pnEventMeta.setDiscoveredAddress(pnEventMeta.getDiscoveredAddress());
        pnEventMeta.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        return pnEventMeta;
    }

}
