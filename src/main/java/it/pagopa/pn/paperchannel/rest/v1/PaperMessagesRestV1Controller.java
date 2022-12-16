package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.rest.v1.api.PaperMessagesApi;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaperMessagesRestV1Controller implements PaperMessagesApi {

    @Autowired
    private PaperMessagesService paperMessagesService;

    @Override
    public Mono<ResponseEntity<SendEvent>> sendPaperPrepareRequest(String requestId, Mono<PrepareRequest> prepareRequest, ServerWebExchange exchange) {
       return prepareRequest.flatMap(request -> paperMessagesService.preparePaperSync(requestId, request))
                .map(ResponseEntity::ok);
  //      return Mono.just(ResponseEntity.noContent().build());
    }

//    @Override
//    public Mono<ResponseEntity<PrepareEvent>> retrievePaperPrepareRequest(String requestId, ServerWebExchange exchange) {
//        return paperMessagesService.retrivePaperPrepareRequest(requestId).map(ResponseEntity::ok);
//    }
}
