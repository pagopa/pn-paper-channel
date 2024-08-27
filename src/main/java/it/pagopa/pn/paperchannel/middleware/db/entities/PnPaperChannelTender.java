package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@Getter
@Setter
@ToString
@DynamoDbBean
@EqualsAndHashCode
public class PnPaperChannelTender {
    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute("tenderId")}))
    private String tenderId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute("activationDate")}))
    private Instant activationDate;

    @Getter(onMethod = @__({@DynamoDbAttribute("tenderName")}))
    private String tenderName;

    @Getter(onMethod = @__({@DynamoDbAttribute("vat")}))
    private Integer vat;

    @Getter(onMethod = @__({@DynamoDbAttribute("nonDeductibleVat")}))
    private Integer nonDeductibleVat;

    @Getter(onMethod = @__({@DynamoDbAttribute("pagePrice")}))
    private BigDecimal pagePrice;

    @Getter(onMethod = @__({@DynamoDbAttribute("basePriceAR")}))
    private BigDecimal basePriceAR;

    @Getter(onMethod = @__({@DynamoDbAttribute("basePriceRS")}))
    private BigDecimal basePriceRS;

    @Getter(onMethod = @__({@DynamoDbAttribute("basePrice890")}))
    private BigDecimal basePrice890;

    @Getter(onMethod = @__({@DynamoDbAttribute("fee")}))
    private BigDecimal fee;

    @Getter(onMethod = @__({@DynamoDbAttribute("createdAt")}))
    private Instant createdAt;
}