package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import reactor.core.publisher.Mono;

public interface PaperTrackerClient {

    Mono<PnDeliveryRequest> initPaperTracking(String trackingId, String productType, String unifiedDeliveryDriver);
}
