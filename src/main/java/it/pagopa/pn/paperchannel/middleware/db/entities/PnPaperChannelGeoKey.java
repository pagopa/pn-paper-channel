package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import java.time.Instant;


@Getter
@Setter
@ToString
@DynamoDbBean
@EqualsAndHashCode
public class PnPaperChannelGeoKey {
    @DynamoDbPartitionKey
    @DynamoDbAttribute("tenderProductGeokey")
    public String getTenderProductGeokey() {
        return String.join("#", tenderId, product, geokey);
    }

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute("activationDate")}))
    private Instant activationDate;

    @Getter(onMethod = @__({@DynamoDbAttribute("tenderId")}))
    private String tenderId;

    @Getter(onMethod = @__({@DynamoDbAttribute("product")}))
    private String product;

    @Getter(onMethod = @__({@DynamoDbAttribute("geokey")}))
    private String geokey;

    @Getter(onMethod = @__({@DynamoDbAttribute("lot")}))
    private String lot;

    @Getter(onMethod = @__({@DynamoDbAttribute("zone")}))
    private String zone;

    @Getter(onMethod = @__({@DynamoDbAttribute("coverFlag")}))
    private Boolean coverFlag;

    @Getter(onMethod = @__({@DynamoDbAttribute("dismissed")}))
    private Boolean dismissed;

    @Getter(onMethod = @__({@DynamoDbAttribute("createdAt")}))
    private Instant createdAt;
}