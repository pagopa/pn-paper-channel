package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnPaperDeliveryDriver {
    public static final String COL_UNIQUE_CODE = "uniqueCode";
    public static final String COL_TENDER_CODE = "tenderCode";
    public static final String COL_DENOMINATION = "denomination";
    public static final String COL_TAX_ID = "taxId";
    public static final String COL_PHONE_NUMBER = "phoneNumber";
    public static final String COL_FSU = "fsu";
    public static final String COL_AUTHOR = "author";
    public static final String AUTHOR_INDEX = "author-index";
    public static final String COL_START_DATE = "startDate";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_UNIQUE_CODE)}))
    public String uniqueCode;

    @Getter(onMethod = @__({@DynamoDbSortKey,@DynamoDbAttribute(COL_TENDER_CODE)}))
    public String tenderCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DENOMINATION)}))
    public String denomination;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TAX_ID)}))
    public String taxId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PHONE_NUMBER)}))
    public String phoneNumber;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FSU)}))
    public Boolean fsu;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = AUTHOR_INDEX),@DynamoDbAttribute(COL_AUTHOR)}))
    public String author;

    @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = AUTHOR_INDEX),@DynamoDbAttribute(COL_START_DATE)}))
    public Instant startDate;
}
