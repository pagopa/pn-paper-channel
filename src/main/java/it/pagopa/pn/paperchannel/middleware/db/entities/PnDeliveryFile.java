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

    public static final String COL_UUID = "uuid";
    public static final String COL_STATUS = "status";
    public static final String COL_URL = "url";
    public static final String COL_FILENAME = "filename";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_UUID)}))
    public String uuid;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS)}))
    public String status;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_URL)}))
    public String url;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FILENAME)}))
    public String filename;
}
