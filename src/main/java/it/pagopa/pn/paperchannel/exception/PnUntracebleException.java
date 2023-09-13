package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.model.KOReason;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

public class PnUntracebleException extends PnErrorNotSavedInDBException {

    public PnUntracebleException(KOReason koReason) {
        super(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage(), koReason);
    }
}
