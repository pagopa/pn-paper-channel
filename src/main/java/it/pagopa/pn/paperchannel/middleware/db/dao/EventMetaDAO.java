package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventMetaDAO {
    Mono<PnEventMeta> createOrUpdate(PnEventMeta pnEventMeta);

    Mono<PnEventMeta> getDeliveryEventMeta(String metaRequestId, String metaStatusCode);

    Flux<PnEventMeta> findAllByRequestId(String metaRequestId);

    Mono<PnEventMeta> deleteEventMeta(String metaRequestId, String metaStatusCode);
}
