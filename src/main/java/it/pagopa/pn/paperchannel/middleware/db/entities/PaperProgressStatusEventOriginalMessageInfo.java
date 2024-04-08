package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.middleware.db.converter.AttributeValueConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class PaperProgressStatusEventOriginalMessageInfo extends OriginalMessageInfo {

    /* Columns */
    public static final String COL_STATUS_CODE = "statusCode";
    public static final String COL_STATUS_DESCRIPTION = "statusDescription";
    public static final String COL_REGISTERED_LETTER_CODE = "registeredLetterCode";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_STATUS_DATE_TIME = "statusDateTime";
    public static final String COL_CLIENT_REQUEST_TIMESTAMP = "clientRequestTimeStamp";


    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DESCRIPTION)}))
    private String statusDescription;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_LETTER_CODE)}))
    private String registeredLetterCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DATE_TIME)}))
    private Instant statusDateTime;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CLIENT_REQUEST_TIMESTAMP)}))
    private Instant clientRequestTimeStamp;

    @Override
    public AttributeValue getAttributeValue() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();

        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_EVENT_TYPE, this.eventType);
        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_STATUS_CODE, this.statusCode);
        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_STATUS_DESCRIPTION, this.statusDescription);
        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_REGISTERED_LETTER_CODE, this.registeredLetterCode);
        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_PRODUCT_TYPE, this.productType);

        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_STATUS_DATE_TIME, this.statusDateTime != null ? this.statusDateTime.toString() : null);
        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_CLIENT_REQUEST_TIMESTAMP, this.clientRequestTimeStamp != null ? this.clientRequestTimeStamp.toString() : null);

        return AttributeValue.fromM(attributeValueMap);
    }
}
