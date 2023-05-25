package it.pagopa.pn.paperchannel.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.Problem;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProblemError;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler {
    public static final String MDC_TRACE_ID_KEY = "trace_id";

    @ExceptionHandler(JsonMappingException.class)
    public void handle(JsonMappingException e) {
        log.error("Returning HTTP 400 Bad Request {}", e.getMessage());
    }

    @ExceptionHandler(PnGenericException.class)
    public Mono<ResponseEntity<Problem>> handleResponseEntityException(final PnGenericException exception){
        log.warn(exception.toString());
        final Problem problem = new Problem();
        settingTraceId(problem);
        problem.setTitle(exception.getExceptionType().getTitle());
        problem.setDetail(exception.getMessage());
        problem.setStatus(exception.getHttpStatus().value());
        problem.setTimestamp(new Date());
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
        }).collect(Collectors.toList()));
        settingTraceId(problem);
        problem.setTimestamp(new Date());

        return Mono.just(ResponseEntity.status(exception.getHttpStatus()).body(problem));
    }

    @ExceptionHandler(PnExcelValidatorException.class)
    public Mono<ResponseEntity<Problem>> handlePnInputValidatorException(final PnExcelValidatorException exception){
        log.warn(exception.toString());
        final Problem problem = new Problem();
        problem.setType(exception.getErrorType().getTitle());
        problem.setTitle(exception.getErrorType().getTitle());
        problem.setDetail(exception.getErrorType().getMessage());
        problem.setStatus(HttpStatus.BAD_REQUEST.value());
        problem.setErrors(exception.getErrors().stream().map(error-> {
            ProblemError item = new ProblemError();
            item.setCode(error.getRow().toString());
            item.setDetail(error.getCol().toString());
            item.setElement(error.getMessage());
            return item;
        }).collect(Collectors.toList()));
        settingTraceId(problem);
        problem.setTimestamp(new Date());

        return Mono.just(ResponseEntity.status(problem.getStatus()).body(problem));
    }


    private void settingTraceId(Problem problem){
        try {
            problem.setTraceId(MDC.get(MDC_TRACE_ID_KEY));
        } catch (Exception e) {
            log.warn("Cannot get traceid", e);
        }
    }
}
