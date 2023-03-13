package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class PnDiscoveredAddress {
    @Getter(onMethod = @__({@DynamoDbAttribute("name")}))
    private String name;

    @Getter(onMethod = @__({@DynamoDbAttribute("nameRow2")}))
    private String nameRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute("address")}))
    private String address;

    @Getter(onMethod = @__({@DynamoDbAttribute("addressRow2")}))
    private String addressRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute("cap")}))
    private String cap;

    @Getter(onMethod = @__({@DynamoDbAttribute("city")}))
    private String city;

    @Getter(onMethod = @__({@DynamoDbAttribute("city2")}))
    private String city2;

    @Getter(onMethod = @__({@DynamoDbAttribute("pr")}))
    private String pr;

    @Getter(onMethod = @__({@DynamoDbAttribute("country")}))
    private String country;
}
