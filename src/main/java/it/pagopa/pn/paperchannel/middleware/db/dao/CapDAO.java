package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CapDAO {

    Mono<List<PnCap>> getAllCap(String cap);

}
