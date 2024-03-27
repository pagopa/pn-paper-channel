package it.pagopa.pn.paperchannel.rule.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
/**
 * Risultato del filtro per una lista, rispetto a ResultFilter contiene anche l'item
 */
public class ListChainResultFilter<T> extends ResultFilter {

    private T item;
}
