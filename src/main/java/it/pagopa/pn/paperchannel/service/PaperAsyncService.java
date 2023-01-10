package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import reactor.core.publisher.Mono;

public interface PaperAsyncService {

    Mono<DeliveryAsyncModel> prepareAsync(PrepareAsyncRequest prepareAsyncRequest);

}