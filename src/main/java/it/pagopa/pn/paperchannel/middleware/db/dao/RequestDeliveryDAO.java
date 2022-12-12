package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RequestDeliveryDAO {

    Mono<RequestDeliveryEntity> create(RequestDeliveryEntity requestDeliveryEntity);

    Mono<RequestDeliveryEntity> updateData(RequestDeliveryEntity requestDeliveryEntity);

    Mono<RequestDeliveryEntity> getByRequestId(String requestId);

    Mono<RequestDeliveryEntity> getByCorrelationId(String correlationId);

    Flux<RequestDeliveryEntity> getByFiscalCode(String fiscalCode);
}
