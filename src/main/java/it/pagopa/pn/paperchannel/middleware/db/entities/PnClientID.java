package it.pagopa.pn.paperchannel.middleware.db.entities;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnClientID {
    public static final String COL_CLIENT_ID = "clientId";
    public static final String COL_PREFIX_VALUE = "prefixValue";
    public static final String INDEX_PREFIX = "prefix-value-index";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_CLIENT_ID)}))
    private String clientId;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = INDEX_PREFIX), @DynamoDbAttribute(COL_PREFIX_VALUE)}))
    private String prefix;

}
