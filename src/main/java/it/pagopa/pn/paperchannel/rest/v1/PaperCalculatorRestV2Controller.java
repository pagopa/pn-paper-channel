package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.PaperCalculatorApi;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateResponse;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
public class PaperCalculatorRestV2Controller implements PaperCalculatorApi {
    private final PaperCalculatorUtils paperCalculatorUtils;

    public PaperCalculatorRestV2Controller(PaperCalculatorUtils paperCalculatorUtils) {
        this.paperCalculatorUtils = paperCalculatorUtils;
    }

    @Override
    public Mono<ResponseEntity<ShipmentCalculateResponse>> calculateCost(String tenderId,
        Mono<ShipmentCalculateRequest> shipmentCalculateRequest, ServerWebExchange exchange) {
        return shipmentCalculateRequest.flatMap(request -> paperCalculatorUtils.costSimulator(tenderId, request))
                .map(ResponseEntity::ok);
    }
}