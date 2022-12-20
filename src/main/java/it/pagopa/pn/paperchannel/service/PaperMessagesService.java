package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import reactor.core.publisher.Mono;

public interface PaperMessagesService {

    Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest);

    Mono<PrepareEvent> retrievePaperPrepareRequest(String requestId);

}
