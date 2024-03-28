package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode(exclude = { "createdAt", "updateAt" } )
public class PnAttachmentsConfig {

    public static final String COL_CONFIG_KEY = "configKey";

    public static final String COL_START_VALIDITY = "startValidity";

    public static final String COL_END_VALIDITY = "endValidity";

    public static final String COL_RULES = "rules";

    public static final String COL_PARENT_REFERENCE = "parentReference";

    public static final String COL_CREATED_AT = "createdAt";

    public static final String COL_UPDATE_AT = "updateAt";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_CONFIG_KEY)}))
    private String configKey;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_START_VALIDITY)}))
    private Instant startValidity;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_END_VALIDITY)}))
    private Instant endValidity;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_RULES)}))
    private List<PnAttachmentsRule> rules;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PARENT_REFERENCE)}))
    private String parentReference;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CREATED_AT)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UPDATE_AT)}))
    private Instant updateAt;


}
