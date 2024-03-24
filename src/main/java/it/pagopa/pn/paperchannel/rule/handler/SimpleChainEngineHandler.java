package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ResultFilter;

import java.util.List;

public abstract class SimpleChainEngineHandler<T, C, R extends ResultFilter> {
    public R filterItem(C context, T items, RuleHandler<T, C, R> handler){
        // implementare logica filtri
        return null;
    }
}
