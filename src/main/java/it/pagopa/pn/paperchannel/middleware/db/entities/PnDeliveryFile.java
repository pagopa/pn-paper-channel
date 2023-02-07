package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnDeliveryFile {

    private static final String COL_UUID = "uuid";
    private static final String COL_STATUS = "status";
    private static final String COL_URL = "url";
    private static final String COL_FILENAME = "filename";
    private static final String COL_ERROR = "error";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_UUID)}))
    private String uuid;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS)}))
    private String status;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_URL)}))
    private String url;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FILENAME)}))
    private String filename;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ERROR)}))
    private PnErrorMessage errorMessage;

}
