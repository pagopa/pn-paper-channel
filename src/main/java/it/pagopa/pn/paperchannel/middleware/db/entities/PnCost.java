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
public class PnCost {
    public static final String COL_ID_DELIVERY_DRIVER = "idDeliveryDriver";
    public static final String COL_UUID = "uuid";
    public static final String COL_CAP = "cap";
    public static final String CAP_INDEX = "cap-index";
    public static final String COL_ZONE = "zone";
    public static final String ZONE_INDEX = "zone-index";
    public static final String COL_TENDER_CODE = "tenderCode";
    public static final String TENDER_INDEX = "tender-index";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_BASE_PRICE = "basePrice";
    public static final String COL_PAGE_PRICE = "pagePrice";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_ID_DELIVERY_DRIVER)}))
    public String idDeliveryDriver;

    @Getter(onMethod = @__({@DynamoDbSortKey,@DynamoDbAttribute(COL_UUID)}))
    public String uuid;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = CAP_INDEX),@DynamoDbAttribute(COL_CAP)}))
    public String cap;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = ZONE_INDEX),@DynamoDbAttribute(COL_ZONE)}))
    public String zone;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = TENDER_INDEX),@DynamoDbAttribute(COL_TENDER_CODE)}))
    public String tenderCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    public String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE)}))
    public Float basePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PAGE_PRICE)}))
    public Float pagePrice;

}
