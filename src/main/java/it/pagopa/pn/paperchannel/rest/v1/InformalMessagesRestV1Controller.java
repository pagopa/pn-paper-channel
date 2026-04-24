package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.InformalMessagesApi;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InformalPrepareRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InformalPrepareResponse;
import it.pagopa.pn.paperchannel.mapper.PrepareRequestMapper;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

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
                .flatMap(request -> {
                    try {
                        validateInformalPrepareRequest(request);
                    } catch (PnGenericException ex) {
                        return Mono.error(ex);
                    }
                    MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, "INFORMAL_PREPARE_PHASE_" + request.getRequestId());
                    return paperMessagesService.preparePaperSync(request.getRequestId(), prepareRequestMapper.informalPrepareRequestToInternal(request, xClientId))
                            .map(internalResponse -> ResponseEntity.ok(new InformalPrepareResponse(request.getRequestId())))
                            .switchIfEmpty(Mono.just(buildCreatedResponseEntity(exchange, request)));
                });

        return MDCUtils.addMDCToContextAndExecute(responseEntityMono);
    }

    private void validateInformalPrepareRequest(InformalPrepareRequest request) {
        if (!StringUtils.hasText(request.getRequestId())) {
            log.error("Missing required field: requestId");
            throw new PnGenericException(REQUEST_ID_IS_REQUIRED, REQUEST_ID_IS_REQUIRED.getMessage());
        }
        if (CollectionUtils.isEmpty(request.getAttachmentUrls())) {
            log.error("Missing or empty required field: attachmentUrls");
            throw new PnGenericException(ATTACHMENT_URLS_IS_REQUIRED, ATTACHMENT_URLS_IS_REQUIRED.getMessage());
        }
    }

    private ResponseEntity<InformalPrepareResponse> buildCreatedResponseEntity(ServerWebExchange exchange, InformalPrepareRequest request) {

        URI location = UriComponentsBuilder
                .fromUri(exchange.getRequest().getURI())
                .replacePath("/paper-channel-private/v1/b2b/paper-deliveries-prepare/{requestId}")
                .buildAndExpand(request.getRequestId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(new InformalPrepareResponse(request.getRequestId()));
    }
}
