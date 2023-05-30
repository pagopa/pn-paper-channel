package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.model.Address;
import reactor.core.publisher.Mono;

public interface AddressManagerClient {


    Mono<DeduplicatesResponseDto> deduplicates(String correlationId, Address base, Address target);

}
