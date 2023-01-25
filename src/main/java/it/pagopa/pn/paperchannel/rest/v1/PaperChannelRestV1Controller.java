package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.rest.v1.api.DeliveryDriverApi;
import it.pagopa.pn.paperchannel.rest.v1.dto.AllPricesContractorResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableTenderResponseDto;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaperChannelRestV1Controller implements DeliveryDriverApi {
    
    @Autowired
    private PaperChannelService paperChannelService;

    @Override
    public Mono<ResponseEntity<PageableTenderResponseDto>> takeTender(Integer page, Integer size, ServerWebExchange exchange) {
        return DeliveryDriverApi.super.takeTender(page, size, exchange);
    }

    @Override
    public Mono<ResponseEntity<PageableDeliveryDriverResponseDto>> takeDeliveriesDrivers(String tenderCode, Integer page, Integer size, ServerWebExchange exchange) {
        return DeliveryDriverApi.super._takeDeliveriesDrivers(tenderCode, page, size, exchange);
    }

    @Override
    public Mono<ResponseEntity<AllPricesContractorResponseDto>> takePrices(String tenderCode, String deliveryDriverId, ServerWebExchange exchange) {
        return DeliveryDriverApi.super.takePrices(tenderCode, deliveryDriverId, exchange);
    }
}
