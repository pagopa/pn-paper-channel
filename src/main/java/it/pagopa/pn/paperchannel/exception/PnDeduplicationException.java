package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;

@Getter
public class PnDeduplicationException extends PnGenericException {
    private final String geokey;

    public PnDeduplicationException(ExceptionTypeEnum exceptionType, String message, String geokey) {
        super(exceptionType, message);
        this.geokey = geokey;
    }
}
