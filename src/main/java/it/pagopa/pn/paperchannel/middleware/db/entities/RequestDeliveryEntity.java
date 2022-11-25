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
public class RequestDeliveryEntity {

    public static final String COL_REQUEST_ID = "requestId";

    public static final String COL_FISCAL_CODE = "fiscalCode";

    public static final String COL_ADDRESS_HASH = "addressHash";

    public static final String FISCAL_CODE_INDEX = "fiscal-code-index";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_REQUEST_ID)}))
    private String requestId;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = FISCAL_CODE_INDEX),@DynamoDbAttribute(COL_FISCAL_CODE)}))
    private String fiscalCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS_HASH)}))
    private String addressHash;

}
