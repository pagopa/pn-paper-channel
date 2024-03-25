package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ResultFilter;

public abstract class SimpleChainEngineHandler<T, C, R extends ResultFilter> {
    public R filterItem(C context, T item, Handler<T, C, R> handler){
        // implementare logica filtri
        return null;
    }
}
