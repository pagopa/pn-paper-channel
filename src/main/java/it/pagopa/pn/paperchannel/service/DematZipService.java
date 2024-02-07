package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.DematInternalEvent;
import reactor.core.publisher.Mono;

/**
 * Business class that handles attachments with ZIP-type attachments
 */
public interface DematZipService {

    /**
     * Business method used by the internal queue consumer, with eventType {@link it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum#SEND_ZIP_HANDLE}
     *
     * @param dematInternalEvent The payload read as input from the consumer
     * @return an empty Mono
     */
    Mono<Void> handle(DematInternalEvent dematInternalEvent);
}
