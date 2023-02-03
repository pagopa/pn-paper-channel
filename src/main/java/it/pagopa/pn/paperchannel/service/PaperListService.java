package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.CapResponseDto;
import reactor.core.publisher.Mono;

public interface PaperListService {

    Mono<CapResponseDto> getAllCap(String value);
}