package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.api.PaperAddressesApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.PaperAddressRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.PaperAddressResponseDto;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.msclient.DataVaultClient;
import it.pagopa.pn.paperchannel.model.PaperAddressRequestInternalDto;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class DataVaultClientImpl implements DataVaultClient {

    private final PaperAddressesApi paperAddressesApi;

    @Override
    public Mono<PaperAddressResponseDto> createPaperAddress(String iun, PaperAddressRequestInternalDto paperAddressRequestInternalDto, AddressTypeEnum addressTypeEnum) {
        final String PN_DATA_VAULT_DESCRIPTION = "Pn Data Vault create paper address";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, PN_DATA_VAULT_DESCRIPTION);

        PaperAddressRequestDto createPaperAddressRequest = AddressMapper.toPaperAddressRequestDto(paperAddressRequestInternalDto, addressTypeEnum);

        return paperAddressesApi.createPaperAddress(iun, createPaperAddressRequest)
                .doOnError( res -> log.error("Paper address creation error - requestId={}", paperAddressRequestInternalDto.getRequestId()));
    }

}
