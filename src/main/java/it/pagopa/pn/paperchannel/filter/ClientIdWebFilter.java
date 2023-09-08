package it.pagopa.pn.paperchannel.filter;

import it.pagopa.pn.paperchannel.exception.PnFilterClientIdException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.CLIENT_ID_EMPTY;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.CLIENT_ID_NOT_PRESENT;
import static it.pagopa.pn.paperchannel.utils.Const.*;

@CustomLog
@Component
@AllArgsConstructor
public class ClientIdWebFilter implements WebFilter {
    private PnClientDAO pnClientDAO;

    @Override
    public @NotNull Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        List<String> valuesHeader = requestHeaders.get(HEADER_CLIENT_ID);
        if (valuesHeader == null || valuesHeader.isEmpty()){
            return chain.filter(exchange);
        }
        String clientId = valuesHeader.get(0);
        if (StringUtils.isBlank(clientId)){
            throw new PnFilterClientIdException(CLIENT_ID_EMPTY.getTitle(), CLIENT_ID_EMPTY.getMessage(), HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.toString());
        }

        return pnClientDAO.getByClientId(clientId)
                .switchIfEmpty(Mono.error(new PnFilterClientIdException(CLIENT_ID_NOT_PRESENT.getTitle(),
                                                                CLIENT_ID_NOT_PRESENT.getMessage().concat(" ClientId = ").concat(clientId),
                                                                HttpStatus.UNAUTHORIZED.value(),
                                                                HttpStatus.UNAUTHORIZED.toString()))
                )
                .flatMap(pnClientID ->
                    chain.filter(exchange)
                            .doFirst(() -> log.logStartingProcess("START PROCESS FROM WEB FILTER"))
                            .contextWrite(ctx -> {
                                ctx.put(CONTEXT_KEY_CLIENT_ID, pnClientID.getClientId());
                                ctx.put(CONTEXT_KEY_PREFIX_CLIENT_ID, pnClientID.getPrefix());
                                return ctx;
                            })
                            .doFinally(ignored -> log.logEndingProcess("ENDING PROCESS FROM WEB FILTER"))
                );

    }



}
