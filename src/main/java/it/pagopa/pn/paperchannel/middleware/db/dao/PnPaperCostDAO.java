package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelCost;
import reactor.core.publisher.Mono;

public interface PnPaperCostDAO {

    Mono<PnPaperChannelCost> createOrUpdate(PnPaperChannelCost pnPaperChannelCost);

    Mono<PnPaperChannelCost>  getCostByTenderIdProductLotZone(String tenderId, String product, String lot, String zone);


}
