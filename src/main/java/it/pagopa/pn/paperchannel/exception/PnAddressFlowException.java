package it.pagopa.pn.paperchannel.exception;


import lombok.Getter;

@Getter
public class PnAddressFlowException extends RuntimeException {
    private final ExceptionTypeEnum exceptionType;


    public PnAddressFlowException(ExceptionTypeEnum exceptionType){
        super(exceptionType.getMessage());
        this.exceptionType = exceptionType;
    }



}