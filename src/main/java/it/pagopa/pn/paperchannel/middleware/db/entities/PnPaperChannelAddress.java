package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Setter
@ToString
@DynamoDbBean
@EqualsAndHashCode
@NoArgsConstructor
public class PnPaperChannelAddress {

    private static final String COL_REQUEST_ID = "requestId";
    private static final String COL_ADDRESS_TYPE = "addressType";
    private static final String COL_ADDRESS_ID = "addressId";
    private static final String COL_CAP = "cap";
    private static final String COL_CITY = "city";
    private static final String COL_COUNTRY = "country";
    private static final String COL_PR = "pr";
    private static final String COL_TTL = "ttl";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_REQUEST_ID)}))
    private String requestId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_ADDRESS_TYPE)}))
    private String addressType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS_ID)}))
    private String addressId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CAP)}))
    @ToString.Exclude
    private String cap;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CITY)}))
    @ToString.Exclude
    private String city;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PR)}))
    @ToString.Exclude
    private String pr;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_COUNTRY)}))
    @ToString.Exclude
    private String country;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;


}
