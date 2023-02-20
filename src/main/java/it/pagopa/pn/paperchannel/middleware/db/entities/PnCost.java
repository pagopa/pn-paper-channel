package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;
import java.util.Objects;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnCost {
    public static final String COL_DELIVERY_DRIVER_CODE = "driverCode";
    public static final String COL_UUID = "uuidCode";
    public static final String COL_CAP = "cap";
    public static final String COL_ZONE = "zoneType";
    public static final String COL_TENDER_CODE = "tenderCode";
    public static final String TENDER_INDEX = "tender-index";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_BASE_PRICE = "basePrice";
    public static final String COL_PAGE_PRICE = "pagePrice";
    public static final String COL_FSU = "fsu";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_DELIVERY_DRIVER_CODE)}))
    private String deliveryDriverCode;

    @Getter(onMethod = @__({@DynamoDbSortKey,@DynamoDbAttribute(COL_UUID)}))
    private String uuid;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CAP)}))
    private List<String> cap;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ZONE)}))
    private String zone;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = TENDER_INDEX), @DynamoDbAttribute(COL_TENDER_CODE)}))
    private String tenderCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE)}))
    private Float basePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PAGE_PRICE)}))
    private Float pagePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FSU)}))
    private Boolean fsu;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        PnCost pnCost = (PnCost) o;
        return Objects.equals(cap, pnCost.cap) && Objects.equals(zone, pnCost.zone) && tenderCode.equals(pnCost.tenderCode) && productType.equals(pnCost.productType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cap, zone, tenderCode, productType);
    }
}
