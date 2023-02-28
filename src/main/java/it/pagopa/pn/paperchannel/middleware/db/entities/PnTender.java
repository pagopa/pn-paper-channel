package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.rest.v1.dto.TenderDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnTender {
    public static final String COL_TENDER_CODE = "tenderCode";
    public static final String COL_DATE = "date";
    public static final String COL_DESCRIPTION= "description";
    public static final String COL_STATUS = "statusType";
    public static final String COL_AUTHOR = "author";
    public static final String AUTHOR_INDEX = "author-index";
    public static final String COL_START_DATE = "startDate";
    public static final String START_DATE_INDEX = "startDate-index";
    public static final String COL_END_DATE = "endDate";

    @Getter(onMethod = @__({@DynamoDbPartitionKey,@DynamoDbAttribute(COL_TENDER_CODE)}))
    public String tenderCode;

    @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = AUTHOR_INDEX),@DynamoDbAttribute(COL_DATE)}))
    public Instant date;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DESCRIPTION)}))
    public String description;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS)}))
    public String status;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = AUTHOR_INDEX),@DynamoDbAttribute(COL_AUTHOR)}))
    public String author;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_START_DATE)}))
    public Instant startDate;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_END_DATE)}))
    public Instant endDate;

    public String getActualStatus(){
        Instant now = Instant.now();
        if (StringUtils.equals(this.status, TenderDTO.StatusEnum.VALIDATED.getValue()) &&
                startDate.isBefore(now) && endDate.isAfter(now)) return TenderDTO.StatusEnum.IN_PROGRESS.getValue();
        if (StringUtils.equals(this.status, TenderDTO.StatusEnum.VALIDATED.getValue()) && endDate.isBefore(now)) return TenderDTO.StatusEnum.ENDED.getValue();
        return this.status;
    }

}
