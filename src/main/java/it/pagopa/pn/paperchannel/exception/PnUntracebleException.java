package it.pagopa.pn.paperchannel.exception;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

public class PnUntracebleException extends PnGenericException {

    public PnUntracebleException() {
        super(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
    }
}
