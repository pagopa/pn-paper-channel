package it.pagopa.pn.paperchannel.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PnExcelValidatorException extends RuntimeException{

    private final List<ErrorCell> errors;
    private final ExceptionTypeEnum errorType;


    public PnExcelValidatorException(ExceptionTypeEnum errorType, List<ErrorCell> errors){
        this.errorType = errorType;
        this.errors = errors;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ErrorCell {
        private Integer row;
        private Integer col;
        private String message;
    }

}
