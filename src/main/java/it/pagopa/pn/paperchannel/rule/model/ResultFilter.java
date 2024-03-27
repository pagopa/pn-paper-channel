package it.pagopa.pn.paperchannel.rule.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * Risultato del filtro, può essere esteso per allegare informazioni
 */
public class ResultFilter {

    private boolean result;
}
