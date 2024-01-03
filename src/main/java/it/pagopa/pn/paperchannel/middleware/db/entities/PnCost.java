package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(of = {"cap", "zone", "tenderCode", "productType"})
public class PnCost {
    public static final String COL_DELIVERY_DRIVER_CODE = "driverCode";
    public static final String COL_UUID = "uuidCode";
    public static final String COL_CAP = "cap";
    public static final String COL_ZONE = "zoneType";
    public static final String COL_TENDER_CODE = "tenderCode";
    public static final String TENDER_INDEX = "tender-index";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_BASE_PRICE = "basePrice";
    public static final String COL_BASE_PRICE_50 = "basePrice50";
    public static final String COL_BASE_PRICE_100 = "basePrice100";
    public static final String COL_BASE_PRICE_250 = "basePrice250";
    public static final String COL_BASE_PRICE_350 = "basePrice350";
    public static final String COL_BASE_PRICE_1000 = "basePrice1000";
    public static final String COL_BASE_PRICE_2000 = "basePrice2000";
    public static final String COL_PAGE_PRICE = "pagePrice";
    public static final String COL_FSU = "fsu";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_DELIVERY_DRIVER_CODE)}))
    private String deliveryDriverCode;

    @Getter(onMethod = @__({@DynamoDbSortKey,@DynamoDbAttribute(COL_UUID)}))
    private String uuid;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CAP)}))
    @ToString.Exclude
    private List<String> cap;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ZONE)}))
    @ToString.Exclude
    private String zone;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = TENDER_INDEX), @DynamoDbAttribute(COL_TENDER_CODE)}))
    private String tenderCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE)}))
    private BigDecimal basePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE_50)}))
    private BigDecimal basePrice50;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE_100)}))
    private BigDecimal basePrice100;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE_250)}))
    private BigDecimal basePrice250;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE_350)}))
    private BigDecimal basePrice350;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE_1000)}))
    private BigDecimal basePrice1000;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE_2000)}))
    private BigDecimal basePrice2000;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PAGE_PRICE)}))
    private BigDecimal pagePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FSU)}))
    private Boolean fsu;

}
