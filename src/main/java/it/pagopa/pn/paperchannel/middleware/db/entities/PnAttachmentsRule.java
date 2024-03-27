package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode
public class PnAttachmentsRule {

    private static final String COL_RULE_TYPE = "ruleType";

    private static final String COL_PARAMS = "params";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_RULE_TYPE)}))
    private String ruleType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PARAMS)}))
    private PnRuleParams params;
}
