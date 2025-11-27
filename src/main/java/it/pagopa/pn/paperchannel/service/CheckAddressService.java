package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CheckAddressResponse;
import reactor.core.publisher.Mono;

public interface CheckAddressService {
    Mono<CheckAddressResponse> checkAddressRequest(String requestId);
}
