package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ResultFilter;
import it.pagopa.pn.paperchannel.rule.model.RuleEngineResult;
import it.pagopa.pn.paperchannel.rule.model.RuleContext;
import it.pagopa.pn.paperchannel.rule.model.RuleModel;

import java.util.List;

public interface RuleEngineHandler<T, C, R extends ResultFilter> {

    List<R> filterItems(C context, List<T> items, RuleHandler<T, C, R> handler);

    R filterItem(C context, T items, RuleHandler<T, C, R> handler);
}
