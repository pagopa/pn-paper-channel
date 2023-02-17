package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.Objects;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnDeliveryDriver {
    public static final String COL_UNIQUE_CODE = "uniqueCode";
    public static final String COL_TENDER_CODE = "tenderCode";
    public static final String COL_DENOMINATION = "denomination";
    public static final String COL_TAX_ID = "taxId";
    public static final String COL_PHONE_NUMBER = "phoneNumber";
    public static final String COL_FSU = "fsu";
    public static final String COL_BUSINESS_NAME = "businessName";
    public static final String COL_REGISTERED_OFFICE = "registeredOffice";
    public static final String COL_PEC = "pec";
    public static final String COL_FISCAL_CODE = "fiscalCode";
    public static final String COL_AUTHOR = "author";
    public static final String AUTHOR_INDEX = "author-index";
    public static final String TENDER_CODE_INDEX = "tender-index";
    public static final String COL_START_DATE = "startDate";

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_TAX_ID)}))
    public String taxId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UNIQUE_CODE)}))
    public String uniqueCode;

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbSecondaryPartitionKey(indexNames = TENDER_CODE_INDEX), @DynamoDbAttribute(COL_TENDER_CODE)}))
    public String tenderCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DENOMINATION)}))
    public String denomination;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PHONE_NUMBER)}))
    public String phoneNumber;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FSU)}))
    public Boolean fsu;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = AUTHOR_INDEX), @DynamoDbAttribute(COL_AUTHOR)}))
    public String author;

    @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = AUTHOR_INDEX), @DynamoDbAttribute(COL_START_DATE)}))
    public Instant startDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BUSINESS_NAME)}))
    private String businessName;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_OFFICE)}))
    private String registeredOffice;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PEC)}))
    private String pec;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FISCAL_CODE)}))
    private String fiscalCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PnDeliveryDriver that = (PnDeliveryDriver) o;
        return uniqueCode.equals(that.uniqueCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueCode);
    }
}