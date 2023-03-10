package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@DynamoDbBean
public class PnEventDemat {
    private static final String COL_PK = "pk";
    private static final String COL_SK = "sk";

    private static final String COL_REQUEST_ID = "requestId";
    private static final String COL_STATUS_CODE = "statusCode";

    private static final String COL_TTL = "ttl";

    private static final String COL_DOCUMENT_TYPE = "documentType";
    private static final String COL_DOCUMENT_DATE = "documentDate";
    private static final String COL_STATUS_DATETIME = "statusDateTime";

    private static final String COL_URI = "uri";
    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_PK)}))
    private String dematRequestId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))
    private String documentTypeStatusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REQUEST_ID)}))
    private String requestId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DOCUMENT_TYPE)}))
    private String documentType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DOCUMENT_DATE)}))
    private Instant documentDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DATETIME)}))
    private Instant statusDateTime;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_URI)}))
    private String uri;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;
}
