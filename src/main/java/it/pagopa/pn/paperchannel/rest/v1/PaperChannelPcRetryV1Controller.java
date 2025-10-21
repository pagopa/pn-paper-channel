package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.PcRetryApi;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.service.PcRetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaperChannelPcRetryV1Controller implements PcRetryApi {

    private final PcRetryService pcRetryService;

    public PaperChannelPcRetryV1Controller(PcRetryService pcRetryService) {
        this.pcRetryService = pcRetryService;
    }

    @Override
    public Mono<ResponseEntity<PcRetryResponse>> getPcRetry(String requestId, Boolean checkApplyRasterization, final ServerWebExchange exchange) {
        return pcRetryService.getPcRetry(requestId, checkApplyRasterization)
                .map(ResponseEntity::ok);
    }
}
