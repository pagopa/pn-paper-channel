package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.rest.v1.dto.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TenderDAO {
    Mono<List<PnTender>> getTenders();

    Mono<PnTender> getTender(String tenderCode);

    Mono<PnTender> findActiveTender();

    Mono<PnTender> createOrUpdate(PnTender tender);
    Mono<PnTender> createNewContract(Map<PnDeliveryDriver, List<PnCost>> deliveriesAndCost, PnTender tender);

    Mono<PnTender> getConsolidate(Date startDate, Date endDate);

    Mono<PnTender> deleteTender(String tenderCode);
}
