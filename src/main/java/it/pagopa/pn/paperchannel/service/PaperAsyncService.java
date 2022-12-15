package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import reactor.core.publisher.Mono;

public interface PaperAsyncService {

    Mono<DeliveryAsyncModel> prepareAsync(String requestId, String correlationId, Address address);
}
