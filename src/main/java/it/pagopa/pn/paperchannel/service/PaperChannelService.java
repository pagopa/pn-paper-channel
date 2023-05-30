package it.pagopa.pn.paperchannel.service;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface PaperChannelService {

    Mono<PageableTenderResponseDto> getAllTender(Integer page, Integer size);

    Mono<TenderDetailResponseDTO> getTenderDetails(String tenderCode);

    Mono<DeliveryDriverResponseDTO> getDriverDetails(String tenderCode, String driverCode);

    Mono<FSUResponseDTO> getDetailsFSU(String tenderCode);
    Mono<PageableDeliveryDriverResponseDto> getAllDeliveriesDrivers(String tenderCode, Integer page, Integer size, Boolean fsu);
    Mono<PageableCostResponseDto> getAllCostFromTenderAndDriver(String tenderCode, String driverCode, Integer page, Integer size);
    Mono<PresignedUrlResponseDto> getPresignedUrl();
    Mono<InfoDownloadDTO> downloadTenderFile(String tenderCode, String uuid);

    Mono<NotifyResponseDto> notifyUpload(String tenderCode, NotifyUploadRequestDto uploadRequestDto);

    Mono<TenderCreateResponseDTO> createOrUpdateTender(TenderCreateRequestDTO request);
    Mono<Void> createOrUpdateDriver(String tenderCode, DeliveryDriverDTO request);

    Mono<Void> createOrUpdateCost(String tenderCode, String deliveryDriverCode, CostDTO request);

    Mono<Void> deleteTender(String tenderCode);
    Mono<Void> deleteDriver(String tenderCode, String deliveryDriverId);
    Mono<Void> deleteCost(String tenderCode, String deliveryDriverId, String uuid);
    Mono<TenderCreateResponseDTO> updateStatusTender(String tenderCode, Status status);

    }