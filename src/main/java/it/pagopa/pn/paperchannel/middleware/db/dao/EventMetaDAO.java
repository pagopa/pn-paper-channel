package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventMetaDAO {
    Mono<PnEventMeta> create(PnEventMeta pnEventMeta);

    Mono<PnEventMeta> getDeliveryEventMeta(String requestId, String statusCode);

    Flux<PnEventMeta> findAllByRequestId(String requestId);

    Mono<PnEventMeta> deleteEventMeta(String requestId, String statusCode);
}
