package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class PnErrorDetails {

    private static final String COL_ROW = "row";
    private static final String COL_COL = "col";
    private static final String COL_COL_NAME = "colName";
    private static final String COL_MESSAGE = "message";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ROW)}))
    private Integer row;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_COL)}))
    private Integer col;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_MESSAGE)}))
    private String message;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_COL_NAME)}))
    private String colName;

}
