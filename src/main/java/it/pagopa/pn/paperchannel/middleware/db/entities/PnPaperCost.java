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
public class PnPaperCost {
    public static final String COL_ID_DELIVERY_DRIVER = "idDeliveryDriver";
    public static final String COL_UUID = "uuid";
    public static final String COL_CAP = "cap";
    public static final String COL_CAP_INDEX = "cap-index";
    public static final String COL_ZONE = "zone";
    public static final String COL_ZONE_INDEX = "zone-index";
    public static final String COL_ID_TENDER = "idTender";
    public static final String COL_TENDER_INDEX = "tender-index";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_BASE_PRICE = "basePrice";
    public static final String COL_PAGE_PRICE = "pagePrice";
    public static final String COL_START_DATE = "startDate";
    public static final String COL_END_DATE = "endDate";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_ID_DELIVERY_DRIVER)}))
    private String idDeliveryDriver;

    @Getter(onMethod = @__({@DynamoDbSortKey,@DynamoDbAttribute(COL_UUID)}))
    private String uuid;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = COL_CAP_INDEX),@DynamoDbAttribute(COL_CAP)}))
    private String cap;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = COL_ZONE_INDEX),@DynamoDbAttribute(COL_ZONE)}))
    private String zone;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = COL_TENDER_INDEX),@DynamoDbAttribute(COL_ID_TENDER)}))
    private String idTender;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BASE_PRICE)}))
    private String basePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PAGE_PRICE)}))
    private String pagePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_START_DATE)}))
    private String startDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_END_DATE)}))
    private String endDate;

}
