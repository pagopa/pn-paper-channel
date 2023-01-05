package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import reactor.core.publisher.Mono;

public interface ZoneDAO {
    Mono<PnZone> getByCountry (String country);
}
