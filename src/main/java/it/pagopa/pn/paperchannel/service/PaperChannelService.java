package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import reactor.core.publisher.Mono;

public interface PaperChannelService {

    Mono<PageableTenderResponseDto> getAllTender(Integer page, Integer size);
    Mono<PageableDeliveryDriverResponseDto> getAllDeliveriesDrivers(String tenderCode, Integer page, Integer size);
    Mono<AllPricesContractorResponseDto> getAllPricesOfDeliveryDriver(String tenderCode, String deliveryDriver);
    Mono<PresignedUrlResponseDto> getPresignedUrl();
    Mono<InfoDownloadDTO> downloadTenderFile(String tenderCode, String uuid);

}