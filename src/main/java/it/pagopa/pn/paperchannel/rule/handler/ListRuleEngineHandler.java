package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ListChainContext;
import it.pagopa.pn.paperchannel.rule.model.ListChainResultFilter;
import it.pagopa.pn.paperchannel.rule.model.RuleModel;

import java.util.List;

public class ListRuleEngineHandler<U extends List<RuleModel>, T, C extends ListChainContext<T>, R extends ListChainResultFilter<T>>  {

    private ListChainEngineHandler<T, C, R> listChainEngineHandler;

     public List<R> filterItems(C context, List<T> items, U rules){
         // implementare logica di risoluzione degli handler
         RuleHandler<T, C, R> handler = null;
         return listChainEngineHandler.filterItems(context, items, handler);
     }
}
