package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.middleware.db.converter.OriginalMessageInfoConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode
public class PnEventError {

    /* Columns */
    public static final String COL_REQUEST_ID = "requestId";
    public static final String COL_STATUS_BUSINESS_DATE_TIME = "statusBusinessDateTime";
    public static final String COL_IUN = "iun";
    public static final String COL_STATUS_CODE = "statusCode";
    public static final String COL_ORIGINAL_MESSAGE_INFO = "originalMessage";
    public static final String COL_CREATED_AT = "createdAt";
    public static final String COL_DRIVER_CODE = "driverCode";
    public static final String COL_TENDER_CODE = "tenderCode";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_REQUEST_ID)}))
    private String requestId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_STATUS_BUSINESS_DATE_TIME)}))
    private Instant statusBusinessDateTime;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_IUN)}))
    private String iun;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_ORIGINAL_MESSAGE_INFO),
            @DynamoDbConvertedBy(value = OriginalMessageInfoConverter.class)
    }))
    private OriginalMessageInfo originalMessageInfo;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CREATED_AT)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DRIVER_CODE)}))
    private String driverCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TENDER_CODE)}))
    private String tenderCode;
}
