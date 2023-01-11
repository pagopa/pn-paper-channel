package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import reactor.core.publisher.Mono;

public interface PaperAsyncService {

    Mono<PnDeliveryRequest> prepareAsync(PrepareAsyncRequest prepareAsyncRequest);

}