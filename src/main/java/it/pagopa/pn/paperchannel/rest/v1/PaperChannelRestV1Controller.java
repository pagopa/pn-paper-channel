package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.rest.v1.api.DeliveryDriverApi;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
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
        return this.paperChannelService.getAllTender(page, size).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PageableDeliveryDriverResponseDto>> takeDeliveriesDrivers(String tenderCode, Integer page, Integer size, ServerWebExchange exchange) {
        return this.paperChannelService.getAllDeliveriesDrivers(tenderCode, page, size).map(ResponseEntity::ok) ;
    }

    @Override
    public Mono<ResponseEntity<PresignedUrlResponseDto>> addTenderFromFile(ServerWebExchange exchange) {
        return this.paperChannelService.getPresignedUrl().map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<InfoDownloadDTO>> downloadTenderFile(String tenderCode, String uuid, ServerWebExchange exchange) {
        return this.paperChannelService.downloadTenderFile(tenderCode, uuid).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<NotifyResponseDto>> notifyUpload(Mono<TenderUploadRequestDto> tenderUploadRequestDto, ServerWebExchange exchange) {
        return tenderUploadRequestDto.flatMap(request -> paperChannelService.notifyUpload(request))
                .map(ResponseEntity::ok);
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
    public Mono<ResponseEntity<FSUResponseDTO>> getDetailFSU(String tenderCode, ServerWebExchange exchange) {
        return this.paperChannelService.getDetailsFSU(tenderCode)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PageableCostResponseDto>> getAllCostOfDriverAndTender(String tenderCode, String deliveryDriverId, Integer page, Integer size, ServerWebExchange exchange) {
        return this.paperChannelService.getAllCostFromTenderAndDriver(tenderCode, deliveryDriverId, page, size)
                .map(ResponseEntity::ok);
    }
}
