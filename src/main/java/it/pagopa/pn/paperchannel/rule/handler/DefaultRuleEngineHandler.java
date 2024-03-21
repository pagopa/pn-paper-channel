package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.*;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.rule.model.ResultFilter.ResultFilterEnum.DISCARD;
import static it.pagopa.pn.paperchannel.rule.model.ResultFilter.ResultFilterEnum.SUCCESS;

public class DefaultRuleEngineHandler implements RuleEngineHandler {


    public ResultRuleEngine filter(List<RuleModel> rules, RuleContext ruleContext) {
        List<RuleAttachmentInfo> filteredAttachments = new ArrayList<>();
        ResultRuleEngine resultRuleEngine = new ResultRuleEngine();
        for (RuleAttachmentInfo currentAttachment: ruleContext.getAttachments()) {
            boolean toAdd = true;
            ResultFilter result = null;
            for(int i = 0; i < rules.size(); i++) {
                RuleModel ruleModel = rules.get(i);
                final RuleHandler ruleHandler = getHandler(ruleModel.getRuleType());
                result = ruleHandler.evaluate(ruleModel, currentAttachment, ruleContext, filteredAttachments);

                if(result.getResult() == SUCCESS) {
                    break;
                }
                else if(result.getResult() == DISCARD) {
                    toAdd = false;
                    break;
                }
            }
            if(toAdd) {
                filteredAttachments.add(currentAttachment);
                resultRuleEngine.getAcceptedAttachments().add(result);
            }
            else resultRuleEngine.getDiscardedAttachments().add(result);
        }
        return resultRuleEngine;

    }


    private RuleHandler getHandler(String ruleType) {
        if(ruleType.equals("DOCUMENT_TYPE")) {
            return new DocumentTypeRuleHandler(); //diventa bean singleton
        }
        return null;
    }

}
