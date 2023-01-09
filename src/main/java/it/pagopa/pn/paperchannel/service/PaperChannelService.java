package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.BaseResponse;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractInsertRequestDto;
import reactor.core.publisher.Mono;

public interface PaperChannelService {

    Mono<BaseResponse> createContract(ContractInsertRequestDto request);

}
