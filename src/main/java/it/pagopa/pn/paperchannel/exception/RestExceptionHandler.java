package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.Problem;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(PnGenericException.class)
    public Mono<ResponseEntity<Problem>> handleResponseEntityException(final PnGenericException pnGenericException){
        final Problem problem = new Problem();
        settingTraceId(problem);
        return Mono.just(ResponseEntity.status(pnGenericException.getHttpStatus()).body(problem));
//        return Mono.just(ResponseEntity.ok(problem));
    }


    @ExceptionHandler(PnPaperEventException.class)
    public Mono<ResponseEntity<PaperEvent>> handlePnPaperEventException(final PnPaperEventException paperEventException){
        return Mono.just(ResponseEntity.noContent().build());
    }

/*
     @ExceptionHandler(PnInputValidatorException.class)
     public Mono<ResponseEntity<Problem>> handlePnInputValidatorException(final PnInputValidatorException exception){
        final Problem problem = new Problem();
        settingTraceId(problem);

        //return Mono.just(ResponseEntity.status(exception.getHttpStatus()).body(problem));
         return null;
     }
*/

    private void settingTraceId(Problem problem){
        try {
            problem.setTraceId(MDC.get(MDC_TRACE_ID_KEY));
        } catch (Exception e) {
            log.warn("Cannot get traceid", e);
        }
    }
}
