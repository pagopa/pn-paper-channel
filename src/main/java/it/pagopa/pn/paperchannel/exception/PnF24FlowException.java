package it.pagopa.pn.paperchannel.exception;


import it.pagopa.pn.paperchannel.model.F24Error;
import lombok.Getter;

@Getter
public class PnF24FlowException extends RuntimeException {
    private final ExceptionTypeEnum exceptionType;

    private final F24Error f24Error;

    public PnF24FlowException(ExceptionTypeEnum exceptionType, F24Error f24Error, Throwable cause){
        super(exceptionType.getMessage(), cause);
        this.exceptionType = exceptionType;
        this.f24Error = f24Error;
    }

}