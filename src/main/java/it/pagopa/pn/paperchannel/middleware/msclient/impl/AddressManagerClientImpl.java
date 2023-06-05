package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.api.DeduplicatesAddressServiceApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.model.Address;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ADDRESS_MANAGER_ERROR;

@Slf4j
@Component
@CustomLog
public class AddressManagerClientImpl implements AddressManagerClient {
    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final DeduplicatesAddressServiceApi apiService;

    public AddressManagerClientImpl(PnPaperChannelConfig cfg,
                                    DeduplicatesAddressServiceApi deduplicatesAddressServiceApi) {
        this.pnPaperChannelConfig = cfg;
        this.apiService = deduplicatesAddressServiceApi;
    }

    @Override
    public Mono<DeduplicatesResponseDto> deduplicates(String correlationId, Address base, Address target) {
        String PN_ADDRESS_MANAGER_DESCRIPTION = "Address Manager deduplicates";
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_ADDRESS_MANAGER, PN_ADDRESS_MANAGER_DESCRIPTION, correlationId);
        DeduplicatesRequestDto requestDto = new DeduplicatesRequestDto();
        requestDto.setBaseAddress(AddressMapper.toAnalogAddressManager(base));
        requestDto.setTargetAddress(AddressMapper.toAnalogAddressManager(target));
        requestDto.setCorrelationId(correlationId);
        log.debug("Call Deduplicates with correlationId : {}", correlationId);
        return this.apiService.deduplicates(
                        this.pnPaperChannelConfig.getAddressManagerCxId(),
                        this.pnPaperChannelConfig.getAddressManagerApiKey(),
                        requestDto
                )
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(response -> {
                    log.info("Deduplicates response correlationId : {}", response.getCorrelationId());
                    log.info("Equality : {}", response.getEqualityResult());
                    log.info("Error : {}", response.getError());
                    return response;
                })
                .onErrorResume(ex -> {
                    log.error("Error with deduplicates API with correlation id: {}", correlationId, ex);
                    return Mono.error(new PnAddressFlowException(ADDRESS_MANAGER_ERROR));
                });
    }
}
