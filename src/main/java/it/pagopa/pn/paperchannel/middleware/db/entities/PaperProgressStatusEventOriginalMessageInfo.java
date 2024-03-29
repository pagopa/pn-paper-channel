package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class PaperProgressStatusEventOriginalMessageInfo extends OriginalMessageInfo {

    /* Columns */
    public static final String COL_STATUS_CODE = "statusCode";
    public static final String COL_STATUS_DESCRIPTION = "statusDescription";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DESCRIPTION)}))
    private String statusDescription;

    @Override
    public AttributeValue getAttributeValue() {
        return AttributeValue.fromM(
                Map.of(
                      COL_EVENT_TYPE, AttributeValue.fromS(this.eventType),
                      COL_STATUS_CODE, AttributeValue.fromS(this.statusCode),
                      COL_STATUS_DESCRIPTION, AttributeValue.fromS(this.statusDescription)
                )
        );
    }
}
