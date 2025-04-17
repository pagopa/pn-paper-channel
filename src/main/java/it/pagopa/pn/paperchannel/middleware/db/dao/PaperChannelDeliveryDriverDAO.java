package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import reactor.core.publisher.Mono;

public interface PaperChannelDeliveryDriverDAO {

    Mono<PaperChannelDeliveryDriver> getByDeliveryDriverId(String deliveryDriverId);
}
