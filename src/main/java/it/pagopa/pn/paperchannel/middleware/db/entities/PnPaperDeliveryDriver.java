package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

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
    public static final String COL_CREATED = "created";
    public static final String COL_CREATED_INDEX = "created-index";
    public static final String COL_START_DATE = "startDate";

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

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = COL_CREATED_INDEX),@DynamoDbAttribute(COL_CREATED)}))
    private String created;

    @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = COL_CREATED_INDEX),@DynamoDbAttribute(COL_START_DATE)}))
    private String startDate;
}
