package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CostDAO {
    Mono<PnCost> getByCapOrZoneAndProductType (String cap, String zone, String productType);

    /**
     * @param tenderCode NOT NULL
     * @param deliveryDriver CAN BE NULL
     * @return List of cost from tender
     */
    Mono<List<PnCost>> retrievePrice(String tenderCode, String deliveryDriver);
    Mono<List<PnCost>> retrievePrice(String tenderCode, String deliveryDriver, Boolean isNational);

    Mono<List<PnCost>> createOrUpdate(List<PnCost> entities);
}
