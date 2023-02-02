package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import reactor.core.publisher.Mono;

public interface CapDAO {

    Mono<PnCap> getAllCap (String cap);
}
