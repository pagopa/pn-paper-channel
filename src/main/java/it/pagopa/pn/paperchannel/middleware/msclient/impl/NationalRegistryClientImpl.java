package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.api.AddressApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressOKDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeoutException;

@CustomLog
@Component
public class NationalRegistryClientImpl extends BaseClient implements NationalRegistryClient {

    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final AddressApi addressApi;


    public NationalRegistryClientImpl(PnPaperChannelConfig pnPaperChannelConfig, AddressApi addressApi) {
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.addressApi = addressApi;
    }



    @Override
    public Mono<AddressOKDto> finderAddress(String correlationId, String recipientTaxId, String recipientType) {
        String PN_NATIONAL_REGISTRY_DESCRIPTION = "National Registry finderAddress";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES, PN_NATIONAL_REGISTRY_DESCRIPTION);
        log.debug("Getting fiscalCode {} key", recipientTaxId);
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setCorrelationId(correlationId);
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setTaxId(recipientTaxId);
        filterDto.setReferenceRequestDate(DateUtils.getOffsetDateTimeFromDate(new Date()));
        addressRequestBodyDto.setFilter(filterDto);

        log.debug("pn-national-registries-cx-id : {}", pnPaperChannelConfig.getNationalRegistryCxId());
        return addressApi.getAddresses(recipientType,addressRequestBodyDto, pnPaperChannelConfig.getNationalRegistryCxId())
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(ex);

                });
    }
}
