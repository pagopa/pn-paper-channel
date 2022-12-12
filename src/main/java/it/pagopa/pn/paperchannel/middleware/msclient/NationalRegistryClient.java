package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressOKDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.GetAddressANPROKDto;
import reactor.core.publisher.Mono;

public interface NationalRegistryClient {

    Mono<AddressOKDto> finderAddress(String recipientTaxId,String recipientType);
}
