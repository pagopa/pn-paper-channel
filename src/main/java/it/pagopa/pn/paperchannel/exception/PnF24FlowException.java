package it.pagopa.pn.paperchannel.exception;


import lombok.Getter;

@Getter
public class PnF24FlowException extends RuntimeException {
    private final ExceptionTypeEnum exceptionType;


    public PnF24FlowException(ExceptionTypeEnum exceptionType){
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
    }



}