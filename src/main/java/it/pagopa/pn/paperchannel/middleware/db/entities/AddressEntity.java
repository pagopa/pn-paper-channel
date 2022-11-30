package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Getter
@Setter
@ToString
public class AddressEntity {

    private static final String COL_FULL_NAME = "fullName";

    private static final String COL_NAME_ROW_2 = "nameRow2";

    private static final String COL_ADDRESS = "address";

    private static final String COL_ADDRESS_ROW_2 = "addressRow2";

    private static final String COL_CAP = "cap";

    private static final String COL_CITY = "city";

    private static final String COL_CITY2 = "city2";

    private static final String COL_PR = "pr";

    private static final String COL_COUNTRY = "country";


    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FULL_NAME)}))
    private String fullName;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_NAME_ROW_2)}))
    private String nameRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS)}))
    private String address;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ADDRESS_ROW_2)}))
    private String addressRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CAP)}))
    private String cap;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CITY)}))
    private String city;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CITY2)}))
    private String city2;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PR)}))
    private String pr;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_COUNTRY)}))
    private String country;

}
