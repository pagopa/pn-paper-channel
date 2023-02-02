package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.rest.v1.api.SelectListApi;
import it.pagopa.pn.paperchannel.rest.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaperListRestV1Controller implements SelectListApi {
    
    @Autowired
    private PaperChannelService paperChannelService;

    @Override
    public Mono<ResponseEntity<CapResponseDto>> getAllCap(String cap, ServerWebExchange exchange) {
        return SelectListApi.super.getAllCap(cap, exchange);
    }
}
