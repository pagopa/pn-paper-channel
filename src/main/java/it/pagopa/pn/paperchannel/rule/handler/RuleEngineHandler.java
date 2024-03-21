package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ResultRuleEngine;
import it.pagopa.pn.paperchannel.rule.model.RuleContext;
import it.pagopa.pn.paperchannel.rule.model.RuleModel;

import java.util.List;

public interface RuleEngineHandler {

    ResultRuleEngine filter(List<RuleModel> rules, RuleContext ruleContext);
}
