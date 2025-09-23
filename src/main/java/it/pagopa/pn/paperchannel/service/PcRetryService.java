package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import reactor.core.publisher.Mono;

public interface PcRetryService {

    Mono<PcRetryResponse> getPcRetry(String requestId, Boolean checkApplyRasterization);
}
