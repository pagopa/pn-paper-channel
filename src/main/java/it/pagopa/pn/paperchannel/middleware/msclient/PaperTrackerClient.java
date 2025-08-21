package it.pagopa.pn.paperchannel.middleware.msclient;

import reactor.core.publisher.Mono;

public interface PaperTrackerClient {

    Mono<Void> initPaperTracking(String trackingId, String productType, String unifiedDeliveryDriver);
}
