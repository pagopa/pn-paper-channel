package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

// handler salva metadati (vedere Gestione eventi di pre-esito)
@RequiredArgsConstructor
@Slf4j
public class SaveMetadataMessageHandler implements MessageHandler {

    protected final EventMetaDAO eventMetaDAO;

    private final Long ttlDays;


    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] Saving PaperRequest from ExcChannel", paperRequest.getRequestId());
        PnEventMeta pnEventMeta = buildPnEventMeta(paperRequest);
        return eventMetaDAO.createOrUpdate(pnEventMeta)
                .doOnNext(savedEntity -> log.info("[{}] Saved PaperRequest from ExcChannel: {}", paperRequest.getRequestId(), savedEntity))
                .then();
    }

    protected PnEventMeta buildPnEventMeta(PaperProgressStatusEventDto paperRequest) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(buildMetaRequestId(paperRequest.getRequestId()));
        pnEventMeta.setMetaStatusCode(buildMetaStatusCode(paperRequest.getStatusCode()));
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDays).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(paperRequest.getStatusCode());
        pnEventMeta.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());

        if (paperRequest.getDiscoveredAddress() != null)
        {
            PnDiscoveredAddress discoveredAddress = new BaseMapperImpl<>(DiscoveredAddressDto.class, PnDiscoveredAddress.class).toDTO(paperRequest.getDiscoveredAddress());
            pnEventMeta.setDiscoveredAddress(discoveredAddress);

            log.info("[{}] Discovered Address in PaperRequest, statusCode {}", paperRequest.getRequestId(), paperRequest.getStatusCode());
        }

        pnEventMeta.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        return pnEventMeta;
    }
}
