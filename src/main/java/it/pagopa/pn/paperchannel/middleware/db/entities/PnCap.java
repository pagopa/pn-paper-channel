package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnCap {

    public static final String AUTHOR_INDEX = "author";
    public static final String COL_CAP = "cap";
    public static final String COL_CITY = "city";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(AUTHOR_INDEX)}))
    public String author;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_CAP)}))
    @ToString.Exclude
    public String cap;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CITY)}))
    @ToString.Exclude
    public String city;
}
