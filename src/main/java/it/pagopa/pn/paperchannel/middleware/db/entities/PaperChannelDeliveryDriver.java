package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@ToString
public class PaperChannelDeliveryDriver {

    public static final String COL_DELIVERY_DRIVER_ID = "deliveryDriverId";
    public static final String COL_UNIFIED_DELIVERY_DRIVER = "unifiedDeliveryDriver";
    public static final String COL_BUSINESS_NAME = "businessName";
    public static final String COL_CREATED_AT = "createdAt";
    public static final String COL_FISCAL_CODE = "fiscalCode";
    public static final String COL_PEC = "pec";
    public static final String COL_PHONE_NUMBER = "phoneNumber";
    public static final String COL_REGISTERED_OFFICE = "registeredOffice";
    public static final String COL_TAX_ID = "taxId";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_DELIVERY_DRIVER_ID)}))
    private String deliveryDriverId;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UNIFIED_DELIVERY_DRIVER)}))
    private String unifiedDeliveryDriver;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BUSINESS_NAME)}))
    private String businessName;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CREATED_AT)}))
    private Instant createdAt;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FISCAL_CODE)}))
    private String fiscalCode;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PEC)}))
    private String pec;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PHONE_NUMBER)}))
    private String phoneNumber;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_OFFICE)}))
    private String registeredOffice;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TAX_ID)}))
    private String taxId;
}
