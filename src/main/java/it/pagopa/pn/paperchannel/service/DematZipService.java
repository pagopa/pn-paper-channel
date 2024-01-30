package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.DematZipInternalEvent;
import reactor.core.publisher.Mono;

public interface DematZipService {

    Mono<Void> handle(DematZipInternalEvent dematZipInternalEvent);
}
