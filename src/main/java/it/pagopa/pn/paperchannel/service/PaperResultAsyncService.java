package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import reactor.core.publisher.Mono;

public interface PaperResultAsyncService {
    Mono<Void> resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt);
}
