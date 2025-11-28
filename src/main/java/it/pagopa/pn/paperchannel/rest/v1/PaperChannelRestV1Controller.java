package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.DeliveryDriverApi;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PaperChannelRestV1Controller implements DeliveryDriverApi {
    

    private final PaperChannelService paperChannelService;

    @Override
    public Mono<ResponseEntity<PageableTenderResponseDto>> takeTender(Integer page, Integer size, ServerWebExchange exchange) {
        return this.paperChannelService.getAllTender(page, size).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PageableDeliveryDriverResponseDto>> takeDeliveriesDrivers(String tenderCode, Integer page, Integer size, Boolean fsu, ServerWebExchange exchange) {
        return this.paperChannelService.getAllDeliveriesDrivers(tenderCode, page, size, fsu).map(ResponseEntity::ok) ;
    }

    @Override
    public Mono<ResponseEntity<TenderCreateResponseDTO>> createUpdateTender(Mono<TenderCreateRequestDTO> tenderCreateRequestDTO, ServerWebExchange exchange) {
        return tenderCreateRequestDTO
                .flatMap(request -> paperChannelService.createOrUpdateTender(request))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> createUpdateDriver(String tenderCode, Mono<DeliveryDriverDTO> deliveryDriverDto, ServerWebExchange exchange) {
        return deliveryDriverDto.flatMap(request -> this.paperChannelService.createOrUpdateDriver(tenderCode, request))
                .map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<Void>> createUpdateCost(String tenderCode, String deliveryDriverId, Mono<CostDTO> costDTO, ServerWebExchange exchange) {
        return costDTO.flatMap(request -> this.paperChannelService.createOrUpdateCost(tenderCode, deliveryDriverId, request))
                .map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<TenderDetailResponseDTO>> getTenderDetails(String tenderCode, ServerWebExchange exchange) {
        return this.paperChannelService.getTenderDetails(tenderCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<DeliveryDriverResponseDTO>> getDriverDetails(String tenderCode, String deliveryDriverId, ServerWebExchange exchange) {
        return this.paperChannelService.getDriverDetails(tenderCode, deliveryDriverId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<FSUResponseDTO>> getDetailFSU(String tenderCode, ServerWebExchange exchange) {
        return this.paperChannelService.getDetailsFSU(tenderCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PageableCostResponseDto>> getAllCostOfDriverAndTender(String tenderCode, String deliveryDriverId, Integer page, Integer size, ServerWebExchange exchange) {
        return this.paperChannelService.getAllCostFromTenderAndDriver(tenderCode, deliveryDriverId, page, size)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteTender(String tenderCode, ServerWebExchange exchange) {
        return this.paperChannelService.deleteTender(tenderCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteDriver(String tenderCode, String deliveryDriverId, ServerWebExchange exchange) {
        return this.paperChannelService.deleteDriver(tenderCode,deliveryDriverId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteCost(String tenderCode, String deliveryDriverId, String uuid, ServerWebExchange exchange) {
        return this.paperChannelService.deleteCost(tenderCode,deliveryDriverId,uuid)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TenderCreateResponseDTO>> updateStatusTender(String tenderCode, Mono<Status> status, ServerWebExchange exchange) {
        return status.flatMap(request -> paperChannelService.updateStatusTender(tenderCode, request))
                .map(ResponseEntity::ok);
    }
}
