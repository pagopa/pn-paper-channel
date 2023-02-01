package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface CostDAO {
    Mono<PnCost> getByCapOrZoneAndProductType (String cap, String zone, String productType);
    Mono<PnTender> createNewContract(Map<PnDeliveryDriver, List<PnCost>> deliveriesAndCost, PnTender tender);
    Mono<List<PnCost>> retrievePrice(String tenderCode, String deliveryDriver);
}
