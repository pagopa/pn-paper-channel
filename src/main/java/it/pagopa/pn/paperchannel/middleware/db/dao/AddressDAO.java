package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import reactor.core.publisher.Mono;

public interface AddressDAO {
    Mono<PnAddress> create (PnAddress pnAddress);

    Mono<PnAddress> findByRequestId (String requestId);
}
