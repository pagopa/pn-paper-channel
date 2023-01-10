package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.rest.v1.api.DeliveryDriverApi;
import it.pagopa.pn.paperchannel.rest.v1.dto.BaseResponse;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractInsertRequestDto;
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
    public Mono<ResponseEntity<BaseResponse>> addContract(Mono<ContractInsertRequestDto> contractInsertRequestDto, ServerWebExchange exchange) {
        return contractInsertRequestDto.flatMap(request -> paperChannelService.createContract(request))
                .map(ResponseEntity::ok);
    }
}
