package it.pagopa.pn.paperchannel.rule.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Contesto base per un filtro con una lista di oggetti
 * Prevede la presenza nel contesto dell'intera lista degli oggetti da valutare
 * e la lista degli attuali risultati valutati
 *
 * @param <T> oggetto da valutare
 */
@Data
public abstract class ListChainContext<T extends Serializable> implements Serializable {

    protected List<T> items;

    protected List<ListChainResultFilter<T>> actualResults;

}
