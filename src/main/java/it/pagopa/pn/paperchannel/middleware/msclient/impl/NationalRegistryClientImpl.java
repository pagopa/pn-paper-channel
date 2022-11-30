package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.PnMicroservicesConfig;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.ApiClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.api.AddressAnprApi;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.GetAddressANPROKDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.GetAddressANPRRequestBodyDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.GetAddressANPRRequestBodyFilterDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class NationalRegistryClientImpl extends BaseClient implements NationalRegistryClient {

    private final PnMicroservicesConfig pnMicroservicesConfig;

    private AddressAnprApi addressAnprApi;

    public NationalRegistryClientImpl(PnMicroservicesConfig pnMicroservicesConfig, AddressAnprApi addressAnprApi) {
        this.pnMicroservicesConfig = pnMicroservicesConfig;
        this.addressAnprApi = addressAnprApi;
    }

    @PostConstruct
    public void init(){
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(this.pnMicroservicesConfig.getUrls().getSafeStorage());
        this.addressAnprApi = new AddressAnprApi(newApiClient);
    }

    @Override
    public Mono<GetAddressANPROKDto> finderAddress(String fiscalCode) {
          log.debug("Getting fiscalCode {} key", fiscalCode);

        GetAddressANPRRequestBodyDto body = new GetAddressANPRRequestBodyDto();
        GetAddressANPRRequestBodyFilterDto filter = new GetAddressANPRRequestBodyFilterDto();
        filter.setTaxId(fiscalCode);
        filter.setReferenceRequestDate("");
        filter.setRequestReason("");
        body.setFilter(filter);
        return addressAnprApi.addressANPR(body)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(ex);
                    /*
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND){
                        return Mono.error(new RaddGenericException(RETRY_AFTER, new BigDecimal(670)));
                    }
                    return Mono.error(new PnSafeStorageException(ex));
                    */
                });
    }
}
