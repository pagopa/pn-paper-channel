package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CostDAO {
    Mono<PnPaperCost> getByCapOrZoneAndProductType (String cap, PnZone zone, String productType);
    Mono<PnPaperDeliveryDriver> createNewContract(PnPaperDeliveryDriver pnDeliveryDriver, List<PnPaperCost> pnListCosts);
    }
