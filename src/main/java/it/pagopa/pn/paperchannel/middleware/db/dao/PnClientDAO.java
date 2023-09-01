package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnClientID;
import reactor.core.publisher.Mono;

public interface PnClientDAO {

    Mono<PnClientID> getByClientId(String clientId);

}
