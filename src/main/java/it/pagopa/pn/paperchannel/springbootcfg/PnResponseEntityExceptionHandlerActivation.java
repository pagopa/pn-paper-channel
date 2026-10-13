package it.pagopa.pn.paperchannel.springbootcfg;


import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnResponseEntityExceptionHandler;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PaperEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProblemError;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;

@ControllerAdvice
@Import(ExceptionHelper.class)
@Slf4j
public class PnResponseEntityExceptionHandlerActivation extends PnResponseEntityExceptionHandler {

    public PnResponseEntityExceptionHandlerActivation(ExceptionHelper exceptionHelper) {
        super(exceptionHelper);
    }


    @ExceptionHandler(PnGenericException.class)
    public Mono<ResponseEntity<Problem>> handleResponseEntityException(final PnGenericException exception){
        log.warn(exception.toString());
        final Problem problem = new Problem();
        settingTraceId(problem);
        problem.setTitle(exception.getExceptionType().getTitle());
        problem.setDetail(exception.getMessage());
        problem.setStatus(exception.getHttpStatus().value());
        problem.setTimestamp(Instant.now());
        return Mono.just(ResponseEntity.status(exception.getHttpStatus()).body(problem));
    }

    @ExceptionHandler(PnPaperEventException.class)
    public Mono<ResponseEntity<PaperEvent>> handlePnPaperEventException(final PnPaperEventException paperEventException){
        return Mono.just(ResponseEntity.noContent().build());
    }

    @ExceptionHandler(PnInputValidatorException.class)
    public Mono<ResponseEntity<Problem>> handlePnInputValidatorException(final PnInputValidatorException exception){
        log.warn(exception.toString());
        final Problem problem = new Problem();
        problem.setTitle(exception.getExceptionType().getTitle());
        problem.setDetail(exception.getExceptionType().getMessage());
        problem.setStatus(exception.getHttpStatus().value());
        problem.setErrors(exception.getErrors().stream().map(error-> {
            ProblemError item = new ProblemError();
            item.setCode("PN_DIFFERENT_DATA");
            item.setDetail("Trovato valore differente a db");
            item.setElement(error);
            return item;
        }).toList());
        settingTraceId(problem);
        problem.setTimestamp(Instant.now());

        return Mono.just(ResponseEntity.status(exception.getHttpStatus()).body(problem));
    }

    private void settingTraceId(Problem problem){
        try {
            problem.setTraceId(MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        } catch (Exception e) {
            log.warn("Cannot get traceid", e);
        }
    }
}
