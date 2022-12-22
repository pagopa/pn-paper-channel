package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import reactor.core.publisher.Mono;

public interface PaperMessagesService {

    Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest);

    Mono<PrepareEvent> retrievePaperPrepareRequest(String requestId);

    Mono<SendResponse> executionPaper(String requestId, SendRequest sendRequest);

}
