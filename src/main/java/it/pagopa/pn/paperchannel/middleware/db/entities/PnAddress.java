package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Getter
@Setter
@ToString
public class PnAddress {

    public static final String ADDRESS_DYNAMO_TABLE_NAME = "AddressDynamoTable";

    private static final String COL_REQUEST_ID = "requestId";

    private static final String COL_FULL_NAME = "fullName";

    private static final String COL_NAME_ROW_2 = "nameRow2";

    private static final String COL_ADDRESS = "address";

    private static final String COL_ADDRESS_ROW_2 = "addressRow2";

    private static final String COL_CAP = "cap";

    private static final String COL_CITY = "city";

    private static final String COL_CITY2 = "city2";

    private static final String COL_PR = "pr";

    private static final String COL_COUNTRY = "country";
    private static final String COL_ADDRESS_TYPE = "addressType";
    private static final String COL_TTL = "ttl";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_REQUEST_ID)}))
    private String requestId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FULL_NAME)}))
    @ToString.Exclude
    private String fullName;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_NAME_ROW_2)}))
    @ToString.Exclude
    private String nameRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS)}))
    @ToString.Exclude
    private String address;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS_ROW_2)}))
    @ToString.Exclude
    private String addressRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CAP)}))
    @ToString.Exclude
    private String cap;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CITY)}))
    @ToString.Exclude
    private String city;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CITY2)}))
    @ToString.Exclude
    private String city2;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PR)}))
    @ToString.Exclude
    private String pr;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_COUNTRY)}))
    @ToString.Exclude
    private String country;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_ADDRESS_TYPE)}))
    private String typology;

}
