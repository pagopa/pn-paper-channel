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
public class PnRuleParams {

    private static final String COL_TYPE_WITH_NEXT_RESULT = "typeWithNextResult";

    private static final String COL_TYPE_WITH_SUCCESS_RESULT = "typeWithSuccessResult";

    private static final String COL_MAX_PAGES_COUNT = "maxPagesCount";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TYPE_WITH_NEXT_RESULT)}))
    private String typeWithNextResult;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TYPE_WITH_SUCCESS_RESULT)}))
    private String typeWithSuccessResult;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_MAX_PAGES_COUNT)}))
    private Integer maxPagesCount;
}
