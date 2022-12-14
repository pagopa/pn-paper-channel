package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;


@Getter
public class PnGenericException extends RuntimeException{
    private final ExceptionTypeEnum exceptionType;
    private final HttpStatus httpStatus;
    private final String message;
    private List<String> errors = null;


    public PnGenericException(ExceptionTypeEnum exceptionType, String message){
        super(message);
        this.exceptionType = exceptionType;
        this.message = message;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public PnGenericException(ExceptionTypeEnum exceptionType, String message,HttpStatus status){
        super(message);
        this.exceptionType = exceptionType;
        this.message = message;
        this.httpStatus = status;
    }

    public PnGenericException(ExceptionTypeEnum exceptionType, String message, HttpStatus status, List<String> list) {
        super(message);
        this.exceptionType = exceptionType;
        this.message = message;
        this.httpStatus = status;
        this.errors = list;
    }
}
