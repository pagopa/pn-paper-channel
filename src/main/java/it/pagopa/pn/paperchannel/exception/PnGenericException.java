package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.model.KOReason;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnGenericException extends RuntimeException {
    private final ExceptionTypeEnum exceptionType;
    private final HttpStatus httpStatus;
    private final String message;
    private KOReason koReason;

    public PnGenericException(ExceptionTypeEnum exceptionType, String message, KOReason koReason){
        this(exceptionType, message);
        this.koReason = koReason;
    }

    public PnGenericException(ExceptionTypeEnum exceptionType, String message){
        this(exceptionType, message, HttpStatus.BAD_REQUEST);
    }

    public PnGenericException(ExceptionTypeEnum exceptionType, String message,HttpStatus status){
        super(message);
        this.exceptionType = exceptionType;
        this.message = message;
        this.httpStatus = status;
    }

}
