package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.PaperMessagesApi;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
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
public class PaperMessagesRestV1Controller implements PaperMessagesApi {

    private final PaperMessagesService paperMessagesService;

    @Override
    public Mono<ResponseEntity<PaperChannelUpdate>> sendPaperPrepareRequest(String requestId, Mono<PrepareRequest> prepareRequest, ServerWebExchange exchange) {
       return prepareRequest
               .doOnNext(request -> {
                   log.debug("Delivery Request of prepare flow");
                   log.debug(request.getReceiverAddress().toString());
               })
               .flatMap(request -> paperMessagesService.preparePaperSync(requestId, request))
               .map(ResponseEntity::ok)
               .switchIfEmpty(Mono.just(ResponseEntity.noContent().build()));
    }

    @Override
    public Mono<ResponseEntity<PrepareEvent>> retrievePaperPrepareRequest(String requestId, ServerWebExchange exchange) {
        return paperMessagesService.retrievePaperPrepareRequest(requestId).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<SendResponse>> sendPaperSendRequest(String requestId, Mono<SendRequest> sendRequest, ServerWebExchange exchange) {
        MDC.put( MDCUtils.MDC_PN_CTX_REQUEST_ID, requestId);
        Mono<ResponseEntity<SendResponse>> responseEntityMono = sendRequest.flatMap(request -> paperMessagesService.executionPaper(requestId, request))
                .map(ResponseEntity::ok);

        return MDCUtils.addMDCToContextAndExecute(responseEntityMono);
    }

    @Override
    public Mono<ResponseEntity<SendEvent>> retrievePaperSendRequest(String requestId, ServerWebExchange exchange) {
        return paperMessagesService.retrievePaperSendRequest(requestId).map(ResponseEntity::ok);
    }
}

