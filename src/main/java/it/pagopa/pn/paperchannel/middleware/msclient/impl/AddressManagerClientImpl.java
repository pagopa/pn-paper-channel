package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.msclient.generated.pnaddressmanager.v1.ApiClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnaddressmanager.v1.api.DeduplicatesAddressServiceApi;
import it.pagopa.pn.paperchannel.msclient.generated.pnaddressmanager.v1.dto.DeduplicatesRequestDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class AddressManagerClientImpl extends BaseClient implements AddressManagerClient {
    private final PnPaperChannelConfig pnPaperChannelConfig;

    private DeduplicatesAddressServiceApi apiService;

    public AddressManagerClientImpl(PnPaperChannelConfig pnPaperChannelConfig) {
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(this.pnPaperChannelConfig.getClientAddressManagerBasepath());
        this.apiService = new DeduplicatesAddressServiceApi(newApiClient);
    }

    @Override
    public Mono<DeduplicatesResponseDto> deduplicates(String correlationId, Address base, Address target) {
        DeduplicatesRequestDto requestDto = new DeduplicatesRequestDto();
        requestDto.setBaseAddress(AddressMapper.toAnalogAddressManager(base));
        requestDto.setTargetAddress(AddressMapper.toAnalogAddressManager(target));
        requestDto.setCorrelationId(correlationId);
        log.debug("Call Deduplicates with correlationId : {}", correlationId);
        return this.apiService.deduplicates("pnAddressMangareCxId", "xApiKey", requestDto)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(response -> {
                    log.debug("Deduplicates response correlationId : {}", response.getCorrelationId());
                    log.debug("Equality : {}", response.getEqualityResult());
                    log.info("Error : {}", response.getError());
                    return response;
                })
                .onErrorResume(ex -> {
                    log.error("Error with deduplicates API with correlation id: {}", correlationId, ex);
                    return Mono.error(ex);
                });
    }
}
