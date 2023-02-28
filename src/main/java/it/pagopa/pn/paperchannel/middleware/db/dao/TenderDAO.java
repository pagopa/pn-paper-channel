package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface TenderDAO {
    Mono<List<PnTender>> getTenders();

    Mono<PnTender> getTender(String tenderCode);

    Mono<PnTender> findActiveTender();

    Mono<PnTender> createOrUpdate(PnTender tender);
    Mono<PnTender> createNewContract(Map<PnDeliveryDriver, List<PnCost>> deliveriesAndCost, PnTender tender);

    Mono<PnTender> getConsolidate(Instant startDate, Instant endDate);

    Mono<PnTender> deleteTender(String tenderCode);
}
