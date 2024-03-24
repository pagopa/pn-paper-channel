package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ListChainContext;
import it.pagopa.pn.paperchannel.rule.model.ListChainResultFilter;

import java.util.ArrayList;
import java.util.List;

public class ListChainEngineHandler<T, C extends ListChainContext<T>, R extends ListChainResultFilter<T>> extends SimpleChainEngineHandler<T, C, R> {

    private SimpleChainEngineHandler<T, C, R> simpleChainEngineHandler;

    public List<R> filterItems(C context, List<T> items, RuleHandler<T, C, R> handler){
        //implementare logica
        List<R> results = new ArrayList<>();
        for (T item:
             items) {
           R r = simpleChainEngineHandler.filterItem(context, item, handler);
           r.setItem(item);
           results.add(r);
        }

        return results;
    }
}
