package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnPaperTender {
    public static final String COL_ID_TENDER = "idTender";
    public static final String COL_DATE = "date";
    public static final String COL_DESCRIPTION= "description";
    public static final String COL_STATUS = "status";
    public static final String COL_AUTHOR = "author";
    public static final String AUTHOR_INDEX = "author-index";
    public static final String COL_START_DATE = "startDate";
    public static final String START_DATE_INDEX = "startDate-index";
    public static final String COL_END_DATE = "endDate";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_ID_TENDER)}))
    public String idTender;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DATE)}))
    public String date;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DESCRIPTION)}))
    public String description;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS)}))
    public String status;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = AUTHOR_INDEX),@DynamoDbAttribute(COL_AUTHOR)}))
    public String author;

    @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = AUTHOR_INDEX),@DynamoDbAttribute(COL_START_DATE)}))
    public Instant startDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_END_DATE)}))
    public String endDate;

}
