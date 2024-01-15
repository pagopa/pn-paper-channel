package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.api.F24ControllerApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.NumberOfPagesResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.PrepareF24RequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import it.pagopa.pn.paperchannel.middleware.msclient.F24Client;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
@CustomLog
public class F24ClientImpl implements F24Client {

    private static final String F24_EXTERNAL_SERVICE = "pn-f24";
    private static final String F_24_GENERATEPDF_DESCRIPTION = "Preparing F24 attachments";
    private static final String F_24_GET_NUMBER_PAGES_DESCRIPTION = "Retrieve pages number of F24 attachments";
    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final F24ControllerApi apiService;

    public F24ClientImpl(PnPaperChannelConfig cfg,
                         F24ControllerApi apiService) {
        this.pnPaperChannelConfig = cfg;
        this.apiService = apiService;
    }



    @Override
    public Mono<RequestAcceptedDto> preparePDF(String requestId, String setId, String recipientIndex, Integer cost) {
        log.logInvokingAsyncExternalService(F24_EXTERNAL_SERVICE, F_24_GENERATEPDF_DESCRIPTION, requestId);
        PrepareF24RequestDto prepareF24RequestDto = new PrepareF24RequestDto();
        prepareF24RequestDto.setRequestId(requestId);
        prepareF24RequestDto.setSetId(setId);
        prepareF24RequestDto.setNotificationCost(cost);
        prepareF24RequestDto.setPathTokens(List.of(recipientIndex));

        return this.apiService.preparePDF(pnPaperChannelConfig.getF24CxId(),
                    requestId,
                    prepareF24RequestDto)
                .map(response -> {
                    log.info("Preparing F24 attachments response successful correlationId={} status={}", requestId, response.getStatus());
                    return response;
                })
                .onErrorResume(ex -> {
                    log.error("Error with Preparing F24 attachments  correlationId={}", requestId, ex);
                    return Mono.error(ex);
                });
    }

    @Override
    public Mono<NumberOfPagesResponseDto> getNumberOfPages(String setId, String recipientIndex) {
        log.logInvokingExternalService(F24_EXTERNAL_SERVICE, F_24_GET_NUMBER_PAGES_DESCRIPTION);

        return this.apiService.getTotalNumberOfPages(setId, List.of(recipientIndex))
                .onErrorResume(ex -> {
                    log.error("Error while getting number of pages from F24 service", ex);
                    return Mono.error(ex);
                });
    }
}
