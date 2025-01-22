package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import reactor.core.publisher.Mono;

public interface PreparePhaseOneAsyncService {

    Mono<PnDeliveryRequest> preparePhaseOneAsync(PrepareNormalizeAddressEvent prepareNormalizeAddressEvent);
}
