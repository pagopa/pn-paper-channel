package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import reactor.core.publisher.Mono;

public interface PaperMessagesService {

    Mono<SendEvent> preparePaperSync(String requestId, Mono<PrepareRequest> prepareRequest);

}
