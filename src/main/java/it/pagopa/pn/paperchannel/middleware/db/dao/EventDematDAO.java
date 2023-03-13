package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventDematDAO {
    Mono<PnEventDemat> createOrUpdate(PnEventDemat pnEventDemat);

    Mono<PnEventDemat> getDeliveryEventDemat(String dematRequestId, String documentTypeStatusCode);

    Flux<PnEventDemat> findAllByRequestId(String dematRequestId);

    Mono<PnEventDemat> deleteEventDemat(String dematRequestId, String documentTypeStatusCode);

    Flux<PnEventDemat> findAllByKeys(String dematRequestId, String... documentTypeStatusCode);
}
