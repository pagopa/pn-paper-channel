package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import reactor.core.publisher.Mono;

public interface PaperChannelService {

    Mono<PageableTenderResponseDto> getAllTender(Integer page, Integer size);

    Mono<TenderDetailResponseDTO> getTenderDetails(String tenderCode);

    Mono<FSUResponseDTO> getDetailsFSU(String tenderCode);
    Mono<PageableDeliveryDriverResponseDto> getAllDeliveriesDrivers(String tenderCode, Integer page, Integer size);
    Mono<PageableCostResponseDto> getAllCostFromTenderAndDriver(String tenderCode, String driverCode, Integer page, Integer size);
    Mono<PresignedUrlResponseDto> getPresignedUrl();
    Mono<InfoDownloadDTO> downloadTenderFile(String tenderCode, String uuid);

    Mono<NotifyResponseDto> notifyUpload(TenderUploadRequestDto uploadRequestDto);

    Mono<TenderCreateResponseDTO> createOrUpdateTender(TenderCreateRequestDTO request);
    Mono<Void> createOrUpdateDriver(String tenderCode, DeliveryDriverDTO request);

    Mono<Void> createOrUpdateCost(String tenderCode, String deliveryDriverCode, CostDTO request);

}