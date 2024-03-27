package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ListChainContext;
import it.pagopa.pn.paperchannel.rule.model.ListChainResultFilter;
import it.pagopa.pn.paperchannel.rule.model.RuleModel;

import java.io.Serializable;
import java.util.List;

/**
 * Implementazione di un valutatore di regole.
 * Traduce in una lista di Handler un set di "regole"
 * TODO: definire il meccanismo di risoluzione delle regole
 *
 * @param <U>
 * @param <T>
 * @param <C>
 * @param <R>
 */
public class ListRuleEngineHandler<U extends List<RuleModel>, T extends Serializable, C extends ListChainContext<T>, R extends ListChainResultFilter<T>>  {

    private ListChainEngineHandler<T, C, R> listChainEngineHandler;

     public List<R> filterItems(C context, List<T> items, U rules){
         // implementare logica di risoluzione degli handler
         Handler<T, C, R> handler = null;
         return listChainEngineHandler.filterItems(context, items, handler);
     }
}
