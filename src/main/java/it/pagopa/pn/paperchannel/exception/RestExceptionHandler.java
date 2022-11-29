package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(PnGenericException.class)
    public Mono<ResponseEntity<Problem>> handleResponseEntityException(final PnGenericException pnGenericException){
        final Problem problem = new Problem();
        return Mono.just(ResponseEntity.ok(problem));
    }


    @ExceptionHandler(PnPaperEventException.class)
    public Mono<ResponseEntity<PaperEvent>> handlePnPaperEventException(final PnPaperEventException paperEventException){
        return Mono.just(ResponseEntity.noContent().build());
    }

}
