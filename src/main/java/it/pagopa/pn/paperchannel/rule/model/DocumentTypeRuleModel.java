package it.pagopa.pn.paperchannel.rule.model;

import lombok.Data;

import java.util.List;

@Data
public class DocumentTypeRuleModel implements RuleModel {

    private List<String> typeWithNextResult;
    private List<String> typeWithSuccesstResult;

    @Override
    public String getRuleType() {
        return "DOCUMENT_TYPE";
    }
}
