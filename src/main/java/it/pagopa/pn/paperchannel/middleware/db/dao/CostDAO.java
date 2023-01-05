package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import reactor.core.publisher.Mono;

public interface CostDAO {
    Mono<PnPaperCost> getByCapOrZoneAndProductType (String cap, PnZone zone, String productType);
}
