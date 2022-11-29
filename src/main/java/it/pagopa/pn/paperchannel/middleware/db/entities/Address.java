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
public class Address {

    @Getter(onMethod = @__({@DynamoDbAttribute("FULLNAME")}))
    private String fullname;
    @Getter(onMethod = @__({@DynamoDbAttribute("NAME_ROW_2")}))
    private String nameRow2;
    @Getter(onMethod = @__({@DynamoDbAttribute("ADDRESS")}))
    private String address;
    @Getter(onMethod = @__({@DynamoDbAttribute("ADDRESS_ROW_2")}))
    private String addressRow2;
    @Getter(onMethod = @__({@DynamoDbAttribute("CAP")}))
    private String cap;
    @Getter(onMethod = @__({@DynamoDbAttribute("CITY")}))
    private String city;
    @Getter(onMethod = @__({@DynamoDbAttribute("CITY2")}))
    private String city2;
    @Getter(onMethod = @__({@DynamoDbAttribute("PR")}))
    private String pr;
    @Getter(onMethod = @__({@DynamoDbAttribute("COUNTRY")}))
    private String country;

}
