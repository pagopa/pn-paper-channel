package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.CategorizedAttachmentsResult;
import it.pagopa.pn.paperchannel.rule.model.RuleContext;
import it.pagopa.pn.paperchannel.rule.model.RuleModel;

import java.util.List;

public interface RuleEngineHandler {

    CategorizedAttachmentsResult filter(List<RuleModel> rules, RuleContext ruleContext);
}
