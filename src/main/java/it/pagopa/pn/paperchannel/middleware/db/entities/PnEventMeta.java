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

@Getter
@Setter
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class PnEventMeta {
    private static final String COL_PK = "pk";
    private static final String COL_SK = "sk";

    private static final String COL_REQUEST_ID = "requestId";
    private static final String COL_STATUS_CODE = "statusCode";

    private static final String COL_TTL = "ttl";

    private static final String COL_DELIVERY_FAILURE_CASE = "deliveryFailureCause";
    private static final String COL_DISCOVERED_ADDRESS = "discoveredAddress";
    private static final String COL_STATUS_DATETIME = "statusDateTime";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_PK)}))
    private String metaRequestId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))
    private String metaStatusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REQUEST_ID)}))
    private String requestId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DELIVERY_FAILURE_CASE)}))
    private String deliveryFailureCause;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DISCOVERED_ADDRESS)}))
    private String discoveredAddress;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DATETIME)}))
    private Instant statusDateTime;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;
}
