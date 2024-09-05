package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import java.time.Instant;


@Setter
@ToString
@DynamoDbBean
@EqualsAndHashCode
public class PnPaperChannelDeliveryDriver {
    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute("deliveryDriverId")}))
    private String deliveryDriverId;

    @Getter(onMethod = @__({@DynamoDbAttribute("taxId")}))
    private String taxId;

    @Getter(onMethod = @__({@DynamoDbAttribute("businessName")}))
    private String businessName;

    @Getter(onMethod = @__({@DynamoDbAttribute("fiscalCode")}))
    private String fiscalCode;

    @Getter(onMethod = @__({@DynamoDbAttribute("pec")}))
    private String pec;

    @Getter(onMethod = @__({@DynamoDbAttribute("phoneNumber")}))
    private String phoneNumber;

    @Getter(onMethod = @__({@DynamoDbAttribute("registeredOffice")}))
    private String registeredOffice;

    @Getter(onMethod = @__({@DynamoDbAttribute("createdAt")}))
    private Instant createdAt;
}
