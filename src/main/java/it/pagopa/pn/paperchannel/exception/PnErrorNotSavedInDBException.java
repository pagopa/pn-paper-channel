package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.model.KOReason;

/**
 * Specializzazione di @{@link PnGenericException} che non viene salvata nella tabella degli errori
 */
public class PnErrorNotSavedInDBException extends PnGenericException {

    public PnErrorNotSavedInDBException(ExceptionTypeEnum exceptionType, String message, KOReason koReason){
        super(exceptionType, message, koReason);
    }
}
