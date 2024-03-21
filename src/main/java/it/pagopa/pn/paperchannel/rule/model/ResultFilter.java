package it.pagopa.pn.paperchannel.rule.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultFilter {

    private ResultFilterEnum result;
    private String reasonCode; //ruleType_code
    private String reasonDescription;
    private String fileKey;

    public enum ResultFilterEnum {

        SUCCESS,
        DISCARD,
        NEXT
    }
}
