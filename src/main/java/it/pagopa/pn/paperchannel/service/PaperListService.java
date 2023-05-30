package it.pagopa.pn.paperchannel.service;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapResponseDto;
import reactor.core.publisher.Mono;

public interface PaperListService {

    Mono<CapResponseDto> getAllCap(String value);
}