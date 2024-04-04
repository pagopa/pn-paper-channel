package it.pagopa.pn.paperchannel.exception;

public class PnInvalidChainRuleException extends RuntimeException {

    public PnInvalidChainRuleException(String message) {
        super(message);
    }

    public PnInvalidChainRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
