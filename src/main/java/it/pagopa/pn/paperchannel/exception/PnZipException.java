package it.pagopa.pn.paperchannel.exception;

public class PnZipException extends RuntimeException {

    public PnZipException(String message) {
        super(message);
    }

    public PnZipException(String message, Throwable cause) {
        super(message, cause);
    }
}
