package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventError;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface PnEventErrorDAO {

    Flux<PnEventError> findEventErrorsByRequestId(String requestId);

    Mono<PnEventError> putItem(PnEventError pnEventError);
    Mono<PnEventError> deleteItem(String requestId, Instant statusBusinessDateTime);
}
