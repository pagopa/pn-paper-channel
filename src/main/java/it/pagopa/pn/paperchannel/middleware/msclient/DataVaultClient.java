package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.PaperAddressResponseDto;
import it.pagopa.pn.paperchannel.model.PaperAddressRequestInternalDto;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import reactor.core.publisher.Mono;

public interface DataVaultClient {

    Mono<PaperAddressResponseDto> createPaperAddress(String iun, PaperAddressRequestInternalDto paperAddressRequestInternalDto, AddressTypeEnum addressTypeEnum);

}