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
public class PnPaperDeliveryDriver {
    public static final String COL_UNIQUE_CODE = "uniqueCode";
    public static final String COL_DENOMINATION = "denomination";
    public static final String COL_TAX_ID = "taxId";
    public static final String COL_PHONE_NUMBER = "phoneNumber";
    public static final String COL_FSU = "fsu";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_UNIQUE_CODE)}))
    private String uniqueCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DENOMINATION)}))
    private String denomination;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TAX_ID)}))
    private String taxId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PHONE_NUMBER)}))
    private String phoneNumber;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FSU)}))
    private Boolean fsu;

}
