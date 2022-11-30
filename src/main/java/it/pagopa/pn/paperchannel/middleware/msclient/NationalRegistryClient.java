package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.GetAddressANPRRequestBodyDto;
import reactor.core.publisher.Mono;

public interface NationalRegistryClient {

    Mono<GetAddressANPROKDto> finderAddress(String fiscalCode);
}
