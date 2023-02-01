package it.pagopa.pn.paperchannel.middleware.db.entities;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnPaperRequestError {
    public static final String COL_REQUEST_ID = "requestId";
    public static final String COL_CREATED = "created";
    public static final String COL_ERROR_TYPE = "error";
    public static final String COL_FLOW_THROW = "flowThrow";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_REQUEST_ID)}))
    public String requestId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_CREATED)}))
    public Instant created;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ERROR_TYPE)}))
    public String error;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FLOW_THROW)}))
    public String flowThrow;
}
