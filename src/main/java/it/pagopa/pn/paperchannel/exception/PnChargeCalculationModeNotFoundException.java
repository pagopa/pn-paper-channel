package it.pagopa.pn.paperchannel.exception;

import java.time.Instant;


public class PnChargeCalculationModeNotFoundException extends RuntimeException {
    public PnChargeCalculationModeNotFoundException(Instant date) {
        super("No mode found for date: " + date);
    }
}
