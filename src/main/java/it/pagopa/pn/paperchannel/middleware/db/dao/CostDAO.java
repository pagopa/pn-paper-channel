package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface CostDAO {
    Mono<PnPaperCost> getByCapOrZoneAndProductType (String cap, String zone, String productType);
    Mono<PnPaperTender> createNewContract(Map<PnPaperDeliveryDriver, List<PnPaperCost>> deliveriesAndCost, PnPaperTender tender);
    Mono<List<PnPaperCost>> retrievePrice(String tenderCode, String deliveryDriver);
}
