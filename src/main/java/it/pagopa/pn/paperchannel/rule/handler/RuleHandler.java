package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.ResultFilter;
import it.pagopa.pn.paperchannel.rule.model.RuleAttachmentInfo;
import it.pagopa.pn.paperchannel.rule.model.RuleContext;
import it.pagopa.pn.paperchannel.rule.model.RuleModel;

import java.util.List;

public interface RuleHandler<T extends RuleModel> {

    ResultFilter evaluate(T ruleModel, RuleAttachmentInfo currentAttachment, RuleContext ruleContext, List<RuleAttachmentInfo> filteredAttachments);
}
