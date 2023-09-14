package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.model.KOReason;
import lombok.Getter;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

@Getter
public class PnUntracebleException extends PnGenericException {
    private final KOReason koReason;

    public PnUntracebleException(KOReason koReason) {
        super(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
        this.koReason = koReason;
    }
}
