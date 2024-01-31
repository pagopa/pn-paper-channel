package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.DematInternalEvent;
import reactor.core.publisher.Mono;

public interface DematZipService {

    Mono<Void> handle(DematInternalEvent dematInternalEvent);
}
