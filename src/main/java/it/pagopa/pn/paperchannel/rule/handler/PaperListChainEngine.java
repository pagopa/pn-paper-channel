package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.commons.rules.ListChainEngineHandler;
import it.pagopa.pn.commons.rules.ListChainHandler;
import it.pagopa.pn.commons.rules.ListRuleEngineHandler;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsRule;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(basePackages = "it.pagopa.pn.commons.rules")
public class PaperListChainEngine extends ListRuleEngineHandler<PnAttachmentsRule, PnAttachmentInfo, PnDeliveryRequest> {

    public PaperListChainEngine(ListChainEngineHandler<PnAttachmentInfo, PnDeliveryRequest> listChainEngineHandler) {
        super(listChainEngineHandler);
    }

    @Override
    protected ListChainHandler<PnAttachmentInfo, PnDeliveryRequest> resolveHandlerFromRule(PnAttachmentsRule ruleModel) {

        if (ruleModel.getRuleType().equals(DocumentTagHandler.RULE_TYPE) )
        {
            return new DocumentTagHandler(ruleModel.getParams());
        }

        throw new PnInvalidChainRuleException("Unknown rule model rule=" + ruleModel);
    }
}
