package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.ApiClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.api.AddressApi;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressOKDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressRequestBodyDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressRequestBodyFilterDto;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class NationalRegistryClientImpl extends BaseClient implements NationalRegistryClient {

    private final PnPaperChannelConfig pnPaperChannelConfig;


    private AddressApi addressApi;

    public NationalRegistryClientImpl(PnPaperChannelConfig pnPaperChannelConfig) {
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(this.pnPaperChannelConfig.getClientNationalRegistriesBasepath());
        this.addressApi = new AddressApi(newApiClient);
    }

    @Override
    public Mono<AddressOKDto> finderAddress(String correlationId, String recipientTaxId,String recipientType) {

        log.debug("Getting fiscalCode {} key", recipientTaxId);
        AddressRequestBodyDto addressRequestBodyDto = new AddressRequestBodyDto();
        AddressRequestBodyFilterDto filterDto = new AddressRequestBodyFilterDto();
        filterDto.setCorrelationId(correlationId);
        filterDto.setDomicileType(AddressRequestBodyFilterDto.DomicileTypeEnum.PHYSICAL);
        filterDto.setTaxId(recipientTaxId);
        filterDto.setReferenceRequestDate(DateUtils.formatDate(new Date()));
        addressRequestBodyDto.setFilter(filterDto);

        return addressApi.getAddresses(recipientType,addressRequestBodyDto)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                //  if(ex.getStatusCode()== HttpStatus.NOT_FOUND){
                  //      return Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage()));
                   // }
                    return Mono.error(ex);

                });
    }
}
