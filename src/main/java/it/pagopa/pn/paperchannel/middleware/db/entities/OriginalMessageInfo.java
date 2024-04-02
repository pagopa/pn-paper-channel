package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@DynamoDbBean
@Data
public class OriginalMessageInfo {

    public static final String COL_EVENT_TYPE = "eventType";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_EVENT_TYPE)}))
    protected String eventType;

    public AttributeValue getAttributeValue() {
        return AttributeValue.fromM(
                Map.of(COL_EVENT_TYPE, AttributeValue.fromS(this.eventType))
        );
    }
}
