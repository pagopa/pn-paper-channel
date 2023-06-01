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
    @ToString.Exclude
    private String name;

    @Getter(onMethod = @__({@DynamoDbAttribute("nameRow2")}))
    @ToString.Exclude
    private String nameRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute("address")}))
    @ToString.Exclude
    private String address;

    @Getter(onMethod = @__({@DynamoDbAttribute("addressRow2")}))
    @ToString.Exclude
    private String addressRow2;

    @Getter(onMethod = @__({@DynamoDbAttribute("cap")}))
    @ToString.Exclude
    private String cap;

    @Getter(onMethod = @__({@DynamoDbAttribute("city")}))
    @ToString.Exclude
    private String city;

    @Getter(onMethod = @__({@DynamoDbAttribute("city2")}))
    @ToString.Exclude
    private String city2;

    @Getter(onMethod = @__({@DynamoDbAttribute("pr")}))
    @ToString.Exclude
    private String pr;

    @Getter(onMethod = @__({@DynamoDbAttribute("country")}))
    @ToString.Exclude
    private String country;
}
