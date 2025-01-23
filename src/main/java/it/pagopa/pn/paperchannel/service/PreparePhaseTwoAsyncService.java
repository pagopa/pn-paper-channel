package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import reactor.core.publisher.Mono;

public interface PreparePhaseTwoAsyncService {
    Mono<PnDeliveryRequest> prepareAsyncPhaseTwo(PnPrepareDelayerToPaperchannelPayload request);
}
