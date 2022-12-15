package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Getter
@Setter
@ToString
public class AttachmentInfoEntity {


    private static final String COL_ID = "id";

    private static final String COL_DOCUMENT_TYPE = "documentType";

    private static final String COL_URL = "url";

    private static final String COL_DATE = "date";

    private static final String COL_FILE_KEY = "fileKey";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ID)}))
    private String id;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DOCUMENT_TYPE)}))
    private String documentType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_URL)}))
    private String url;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DATE)}))
    private String date;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FILE_KEY)}))
    private String fileKey;
}
