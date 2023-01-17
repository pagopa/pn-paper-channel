package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnZone {
    public static final String COL_ZONE = "zone";
    public static final String COL_COUNTRY_EN = "countryEn";
    public static final String COUNTRY_EN_INDEX = "countryEn-index";
    public static final String COL_COUNTRY_IT = "countryIt";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_COUNTRY_IT)}))
    public String countryIt;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = COUNTRY_EN_INDEX),@DynamoDbAttribute(COL_COUNTRY_EN)}))
    public String countryEn;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ZONE)}))
    public String zone;
}
