package it.pagopa.pn.paperchannel.rule.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResultRuleEngine {

    private List<ResultFilter> acceptedAttachments = new ArrayList<>();
    private List<ResultFilter> discardedAttachments = new ArrayList<>();

}
