package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ListChainContext;
import it.pagopa.pn.paperchannel.rule.model.ListChainResultFilter;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Semplice implementazione di un valutatore di filtri su una lista di oggetti.
 * Di fatto, wrappa la chiamata verso l'handler per singolo oggetto, e arricchisce il contesto
 * con i risultati man mano che gli oggetti vengono valutati
 * Il contesto viene clonato prima di ogni step, per evitare che step successivi possano alterare i
 * risultati precedenti.
 *
 * @param <T> vedi Handler
 * @param <C> vedi Handler
 * @param <R> vedi Handler
 */
public class ListChainEngineHandler<T extends Serializable, C extends ListChainContext<T>, R extends ListChainResultFilter<T>> extends SimpleChainEngineHandler<T, C, R> {

    private SimpleChainEngineHandler<T, C, R> simpleChainEngineHandler;

    public List<R> filterItems(C context, List<T> items, Handler<T, C, R> handler){
        //implementare logica
        List<R> results = new ArrayList<>();
        for (T item:
             items) {
            C deepCopyContext = (C) SerializationUtils.clone(context);
            R r = simpleChainEngineHandler.filterItem(deepCopyContext, item, handler);
            r.setItem(item);
            results.add(r);
            context.getActualResults().add(r);
        }

        return results;
    }
}
