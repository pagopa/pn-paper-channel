package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import reactor.core.publisher.Mono;

public interface RequestDeliveryDAO {

    Mono<PnDeliveryRequest> createWithAddress(PnDeliveryRequest request, PnAddress pnAddress, PnAddress discoveredAddress);

    Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest);
    Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest, boolean ignorableNulls);

    Mono<PnDeliveryRequest> getByRequestId(String requestId);
    Mono<PnDeliveryRequest> getByRequestId(String requestId, boolean decode);
    Mono<PnDeliveryRequest> getByCorrelationId(String requestId, boolean decode);
    Mono<PnDeliveryRequest> getByCorrelationId(String correlationId);


}
