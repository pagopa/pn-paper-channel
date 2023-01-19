package it.pagopa.pn.paperchannel.service;


import it.pagopa.pn.paperchannel.rest.v1.dto.AllPricesContractorResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableTenderResponseDto;
import reactor.core.publisher.Mono;

public interface PaperChannelService {


    Mono<PageableTenderResponseDto> getAllTender(Integer page, Integer size);
    Mono<PageableDeliveryDriverResponseDto> getAllDeliveriesDrivers(String tenderCode, Integer page, Integer size);
    Mono<AllPricesContractorResponseDto> getAllPricesOfDeliveryDriver(String tenderCode, String deliveryDriver);


}