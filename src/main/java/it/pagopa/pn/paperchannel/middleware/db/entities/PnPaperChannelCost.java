package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;


@Setter
@ToString
@DynamoDbBean
@EqualsAndHashCode
public class PnPaperChannelCost {
    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute("tenderId")}))
    private String tenderId;

    /**
     * Gets compound key product lot zone.
     *
     * @return the compound key product lot zone
     */
    @DynamoDbSortKey
    @DynamoDbAttribute("productLotZone")
    public String getProductLotZone() {
        return String.join("#", product, lot, zone);
    }

    @Getter(onMethod = @__({@DynamoDbAttribute("product")}))
    private String product;

    @Getter(onMethod = @__({@DynamoDbAttribute("lot")}))
    private String lot;

    @Getter(onMethod = @__({@DynamoDbAttribute("zone")}))
    private String zone;

    @Getter(onMethod = @__({@DynamoDbAttribute("deliveryDriverNam")}))
    private String deliveryDriverName;

    @Getter(onMethod = @__({@DynamoDbAttribute("deliveryDriverId")}))
    private String deliveryDriverId;

    @Getter(onMethod = @__({@DynamoDbAttribute("dematerializationCost")}))
    private BigDecimal dematerializationCost;

    @Getter(onMethod = @__({@DynamoDbAttribute("rangedCosts")}))
    private List<PnRange> rangedCosts;

    @Getter(onMethod = @__({@DynamoDbAttribute("createdAt")}))
    private Instant createdAt;
}