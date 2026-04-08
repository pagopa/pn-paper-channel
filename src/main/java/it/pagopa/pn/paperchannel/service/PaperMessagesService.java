package it.pagopa.pn.paperchannel.service;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.model.PrepareRequestInt;
import reactor.core.publisher.Mono;

public interface PaperMessagesService {

    Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequestInt prepareRequest);

    Mono<PrepareEvent> retrievePaperPrepareRequest(String requestId);

    Mono<SendResponse> executionPaper(String requestId, SendRequest sendRequest);

    Mono<SendEvent> retrievePaperSendRequest(String requestId);


}
