package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import reactor.core.publisher.Mono;

public interface AttachmentsConfigService {

    Mono<Void> refreshConfig(PnAttachmentsConfigEventPayload payload);
}
