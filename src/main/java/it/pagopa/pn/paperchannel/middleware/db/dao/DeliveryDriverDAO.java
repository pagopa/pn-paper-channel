package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DeliveryDriverDAO {

    Mono<List<PnDeliveryDriver>> getDeliveryDriver(String tenderCode);
}
