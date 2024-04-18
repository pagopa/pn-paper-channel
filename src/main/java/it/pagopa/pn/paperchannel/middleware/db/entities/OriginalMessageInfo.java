package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.middleware.db.converter.AttributeValueConverter;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

@DynamoDbBean
@Data
public class OriginalMessageInfo {

    public static final String COL_EVENT_TYPE = "eventType";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_EVENT_TYPE)}))
    protected String eventType;

    public AttributeValue getAttributeValue() {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        AttributeValueConverter.addAttributeValueToMap(attributeValueMap, COL_EVENT_TYPE, this.eventType);

        return AttributeValue.fromM(attributeValueMap);
    }
}
