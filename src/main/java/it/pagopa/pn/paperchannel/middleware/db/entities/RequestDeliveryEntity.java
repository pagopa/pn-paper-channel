package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class RequestDeliveryEntity {

    public static final String COL_REQUEST_ID = "requestId";

    public static final String COL_FISCAL_CODE = "fiscalCode";

    public static final String COL_ADDRESS_HASH = "addressHash";

    public static final String COL_ADDRESS = "address";

    public static final String FISCAL_CODE_INDEX = "fiscal-code-index";

    private static final String COL_STATUS_CODE = "statusCode";

    private static final String COL_STATUS_DETAIL = "statusDetail";

    private static final String COL_STATUS_DATE = "statusDate";

    private static final String COL_REGISTERED_LETTER_CODE = "registeredLetterCode";

    private static final String COL_START_DATE = "startDate";

    private static final String COL_ATTACHMENTS = "attachments";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_REQUEST_ID)}))
    private String requestId;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = FISCAL_CODE_INDEX),@DynamoDbAttribute(COL_FISCAL_CODE)}))
    private String fiscalCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS_HASH)}))
    private String addressHash;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS)}))
    private AddressEntity address;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DETAIL)}))
    private String statusDetail;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DATE)}))
    private String statusDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_LETTER_CODE)}))
    private String registeredLetterCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_START_DATE)}))
    private String startDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ATTACHMENTS)}))
    private List<AttachmentInfoEntity> attachments;

}
