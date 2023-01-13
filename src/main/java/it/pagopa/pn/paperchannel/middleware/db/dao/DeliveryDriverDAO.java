package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.model.DeliveryDriverFilter;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DeliveryDriverDAO {

    Mono<PnPaperDeliveryDriver> addDeliveryDriver(PnPaperDeliveryDriver pnDeliveryDriver);
    Mono<List<PnPaperDeliveryDriver>> getDeliveryDriver(DeliveryDriverFilter filter);
}
