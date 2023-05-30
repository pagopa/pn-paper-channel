package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.PaperMessagesApi;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class PaperMessagesRestV1Controller implements PaperMessagesApi {

    @Autowired
    private PaperMessagesService paperMessagesService;

    @Override
    public Mono<ResponseEntity<PaperChannelUpdate>> sendPaperPrepareRequest(String requestId, Mono<PrepareRequest> prepareRequest, ServerWebExchange exchange) {
       return prepareRequest
               .doOnNext(request -> {
                   log.debug("Delivery Request of prepare flow");
                   log.debug(request.getReceiverAddress().toString());
               })
               .flatMap(request -> paperMessagesService.preparePaperSync(requestId, request))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PrepareEvent>> retrievePaperPrepareRequest(String requestId, ServerWebExchange exchange) {
        return paperMessagesService.retrievePaperPrepareRequest(requestId).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<SendResponse>> sendPaperSendRequest(String requestId, Mono<SendRequest> sendRequest, ServerWebExchange exchange) {
        return sendRequest.flatMap(request -> paperMessagesService.executionPaper(requestId, request))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<SendEvent>> retrievePaperSendRequest(String requestId, ServerWebExchange exchange) {
        return paperMessagesService.retrievePaperSendRequest(requestId).map(ResponseEntity::ok);
    }
}

