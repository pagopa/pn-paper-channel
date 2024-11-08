package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;

@Getter
public class StopFlowSecondAttemptException extends PnGenericException {
    private final String geokey;

    public StopFlowSecondAttemptException(ExceptionTypeEnum exceptionType, String message, String geokey) {
        super(exceptionType, message);
        this.geokey = geokey;
    }
}
