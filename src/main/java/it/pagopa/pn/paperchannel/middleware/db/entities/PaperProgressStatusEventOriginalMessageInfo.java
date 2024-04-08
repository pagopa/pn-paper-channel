package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
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
        return AttributeValue.fromM(
            Map.of(
                COL_EVENT_TYPE, AttributeValue.fromS(this.eventType),
                COL_STATUS_CODE, AttributeValue.fromS(this.statusCode),
                COL_STATUS_DESCRIPTION, AttributeValue.fromS(this.statusDescription),
                COL_REGISTERED_LETTER_CODE, AttributeValue.fromS(this.registeredLetterCode),
                COL_PRODUCT_TYPE, AttributeValue.fromS(this.productType),
                COL_STATUS_DATE_TIME, AttributeValue.fromS(this.statusDateTime != null ? this.statusDateTime.toString() : null),
                COL_CLIENT_REQUEST_TIMESTAMP, AttributeValue.fromS(this.clientRequestTimeStamp != null ? this.clientRequestTimeStamp.toString() : null)
            )
        );
    }
}
