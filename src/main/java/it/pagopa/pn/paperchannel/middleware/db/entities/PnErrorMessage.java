package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class PnErrorMessage {

    private static final String COL_MESSAGE = "message";
    private static final String COL_ERROR_DEATILS = "errorDetails";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_MESSAGE)}))
    private String message;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ERROR_DEATILS)}))
    private List<PnErrorDetails> errorDetails;

}
