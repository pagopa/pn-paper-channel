package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.PaperCalculatorApi;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaperCalculatorRestV2Controller implements PaperCalculatorApi {

    @Override
    public Mono<ResponseEntity<ShipmentCalculateResponse>> calculateCost(String tenderId,
        Mono<ShipmentCalculateRequest> shipmentCalculateRequest, ServerWebExchange exchange) {
        return PaperCalculatorApi.super.calculateCost(tenderId, shipmentCalculateRequest, exchange);
    }
}
