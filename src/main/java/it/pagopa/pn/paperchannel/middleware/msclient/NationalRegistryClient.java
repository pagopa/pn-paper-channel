package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressOKDto;
import reactor.core.publisher.Mono;

public interface NationalRegistryClient {
    Mono<AddressOKDto> finderAddress(String correlationId, String recipientTaxId, String recipientType);
}
