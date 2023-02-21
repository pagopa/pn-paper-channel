package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DeliveryDriverDAO {

    Flux<PnDeliveryDriver> getDeliveryDriverFromTender(String tenderCode, Boolean onlyFSU);
    Mono<PnDeliveryDriver> getDeliveryDriverFSU(String tenderCode);
    Mono<PnDeliveryDriver> getDeliveryDriver(String tenderCode, String taxId);

    Mono<PnDeliveryDriver> createOrUpdate(PnDeliveryDriver data);

}
