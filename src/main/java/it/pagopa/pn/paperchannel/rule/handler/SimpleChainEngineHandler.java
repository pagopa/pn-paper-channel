package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ResultFilter;

/**
 * Semplice implementazione di un valutatore di filtri.
 * Di fatto, wrappa la chiamata verso l'handler
 *
 * @param <T> vedi Handler
 * @param <C> vedi Handler
 * @param <R> vedi Handler
 */
public class SimpleChainEngineHandler<T, C, R extends ResultFilter> {
    public R filterItem(C context, T item, Handler<T, C, R> handler){
        // richiama wrappando il filtro, tipo con log o altro
        return handler.filter(item, context);
    }
}
