package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventDematDAO {
    Mono<PnEventDemat> create(PnEventDemat pnEventDemat);

    Mono<PnEventDemat> getDeliveryEventDemat(String requestId, String statusCode);

    Flux<PnEventDemat> findAllByRequestId(String requestId);

    Mono<PnEventDemat> deleteEventDemat(String requestId, String statusCode);
}
