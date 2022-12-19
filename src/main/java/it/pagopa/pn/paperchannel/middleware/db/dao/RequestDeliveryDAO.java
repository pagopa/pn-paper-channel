package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RequestDeliveryDAO {

    Mono<PnDeliveryRequest> create(PnDeliveryRequest pnDeliveryRequest);

    Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest);

    Mono<PnDeliveryRequest> getByRequestId(String requestId);

    Mono<PnDeliveryRequest> getByCorrelationId(String correlationId);

    Flux<PnDeliveryRequest> getByFiscalCode(String fiscalCode);
}
