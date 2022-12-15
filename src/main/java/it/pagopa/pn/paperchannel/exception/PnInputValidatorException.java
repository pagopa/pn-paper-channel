package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.List;

@Getter
public class PnInputValidatorException extends PnGenericException{
    private final List<String> errors;

    public PnInputValidatorException(ExceptionTypeEnum exceptionType, String message, HttpStatus status, List<String> list) {
        super(exceptionType, message, status);
        this.errors = list;
    }

}
