package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.CheckAddressApi;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CheckAddressResponse;
import it.pagopa.pn.paperchannel.service.CheckAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CheckAddressRestV1Controller implements CheckAddressApi {

    private final CheckAddressService checkAddressService;

    @Override
    public Mono<ResponseEntity<CheckAddressResponse>> checkAddress(String requestId,  final ServerWebExchange exchange) {
        MDC.put( MDCUtils.MDC_PN_CTX_REQUEST_ID, "CHECK_ADDRESS_" + requestId);
        Mono<ResponseEntity<CheckAddressResponse>> responseEntityMono = Mono.just(requestId)
                .flatMap(request -> checkAddressService.checkAddressRequest(requestId))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

        return MDCUtils.addMDCToContextAndExecute(responseEntityMono);
    }
}

