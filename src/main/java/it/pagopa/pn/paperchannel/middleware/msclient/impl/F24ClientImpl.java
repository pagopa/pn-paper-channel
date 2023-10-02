package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.api.F24ControllerApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.PrepareF24RequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import it.pagopa.pn.paperchannel.middleware.msclient.F24Client;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;


@Component
@CustomLog
public class F24ClientImpl implements F24Client {
    private static final String F_24_GENERATEPDF_DESCRIPTION = "Preparing F24 attachments";
    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final F24ControllerApi apiService;

    public F24ClientImpl(PnPaperChannelConfig cfg,
                         F24ControllerApi apiService) {
        this.pnPaperChannelConfig = cfg;
        this.apiService = apiService;
    }



    @Override
    public Mono<RequestAcceptedDto> preparePDF(String requestId, String setId, String recipientIndex, Integer cost) {
        log.logInvokingAsyncExternalService("pn-f24", F_24_GENERATEPDF_DESCRIPTION, requestId);
        PrepareF24RequestDto prepareF24RequestDto = new PrepareF24RequestDto();
        prepareF24RequestDto.setRequestId(requestId);
        prepareF24RequestDto.setSetId(setId);
        prepareF24RequestDto.setNotificationCost(cost);
        prepareF24RequestDto.setPathTokens(List.of(recipientIndex));

        return this.apiService.preparePDF(pnPaperChannelConfig.getF24CxId(),
                requestId,
                prepareF24RequestDto
                )
                .retryWhen(
                Retry.backoff(2, Duration.ofMillis(500))
                        .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
        ).map(response -> {
                    log.info("Preparing F24 attachments response successful correlationId={} status={}", requestId, response.getStatus());
                    return response;
                })
                .onErrorResume(ex -> {
                    log.error("Error with Preparing F24 attachments  correlationId={}", requestId, ex);
                    return Mono.error(ex);
                });
    }
}
