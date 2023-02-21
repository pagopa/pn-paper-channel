package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CostDAO {

    Mono<PnCost> getByCapOrZoneAndProductType(String tenderCode, String cap, String zone, String productType);

    /**
     * @param tenderCode NOT NULL
     * @param driverCode CAN BE NULL
     * @return List of cost from tender
     */
    Flux<PnCost> findAllFromTenderCode(String tenderCode, String driverCode);

    /**
     *
     * @param tenderCode NOT NULL
     * @param productType CAN BE NULL
     * @param uuidExclude CAN BE NULL
     * @return List of cost from tender and product type without cost with uuid
     */
    Mono<List<PnCost>> findAllFromTenderAndProductTypeAndExcludedUUID(String tenderCode, String productType, String uuidExclude);

    Mono<PnCost> createOrUpdate(PnCost entities);
    Mono<PnCost> deleteCost(String deliveryDriverCode, String uuid);
}
