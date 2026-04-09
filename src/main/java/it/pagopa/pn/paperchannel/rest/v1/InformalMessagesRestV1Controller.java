package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.InformalMessagesApi;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InformalPrepareRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InformalPrepareResponse;
import it.pagopa.pn.paperchannel.mapper.PrepareRequestMapper;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.CLIENT_ID_IS_REQUIRED;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InformalMessagesRestV1Controller implements InformalMessagesApi {

    private final PaperMessagesService paperMessagesService;
    private final PrepareRequestMapper prepareRequestMapper;

    @Override
    public Mono<ResponseEntity<InformalPrepareResponse>> sendInformalPrepareRequest(Mono<InformalPrepareRequest> informalPrepareRequest, String xClientId, ServerWebExchange exchange) {
        if (!StringUtils.hasText(xClientId)) {
            log.error("Missing required header: xClientId");
            return Mono.error(new PnGenericException(CLIENT_ID_IS_REQUIRED, CLIENT_ID_IS_REQUIRED.getMessage()));
        }

        Mono<ResponseEntity<InformalPrepareResponse>> responseEntityMono = informalPrepareRequest
                .doOnNext(request -> {
                    MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, "PREPARE_PHASE_" + request.getRequestId());
                    log.info("Informal request of prepare flow");
                    log.debug("Receiver address: {}", request.getReceiverAddress());
                })
                .flatMap(request -> paperMessagesService.preparePaperSync(request.getRequestId(), prepareRequestMapper.informalPrepareRequestToInternal(request, xClientId))
                                .map(internalResponse -> ResponseEntity.ok(new InformalPrepareResponse(request.getRequestId())))
                                .switchIfEmpty(Mono.just(buildCreatedResponseEntity(exchange, request)))
                );

        return MDCUtils.addMDCToContextAndExecute(responseEntityMono);
    }

    private ResponseEntity<InformalPrepareResponse> buildCreatedResponseEntity(ServerWebExchange exchange, InformalPrepareRequest request) {
        return ResponseEntity.created(buildLocationUri(exchange)).body(new InformalPrepareResponse(request.getRequestId()));
    }


    private URI buildLocationUri(ServerWebExchange exchange) {
        return UriComponentsBuilder
                .fromUri(exchange.getRequest().getURI())
                .replacePath("/paper-channel-private/v1/paper-deliveries-prepare/informal")
                .build()
                .toUri();
    }
}
