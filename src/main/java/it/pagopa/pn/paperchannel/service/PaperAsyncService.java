package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.pojo.Address;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import reactor.core.publisher.Mono;

public interface PaperAsyncService {

    Mono<DeliveryPayload> prepareAsync(String requestId, String correlationId, Address address);
}
