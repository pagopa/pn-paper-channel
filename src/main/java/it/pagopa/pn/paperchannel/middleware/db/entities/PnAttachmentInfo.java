package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.io.Serializable;

@DynamoDbBean
@Getter
@Setter
@ToString
public class PnAttachmentInfo implements Serializable {


    private static final String COL_ID = "id";

    private static final String COL_DOCUMENT_TYPE = "documentType";

    private static final String COL_URL = "url";

    private static final String COL_DATE = "date";

    private static final String COL_FILE_KEY = "fileKey";

    private static final String COL_PAGE_NUMBER = "numberOfPage";

    private static final String COL_CHECKSUM = "checksum";

    private static final String COL_GENERATEDFROM = "generatedFrom";

    private static final String COL_DOC_TAG = "docTag";

    private static final String COL_FILTER_RESULT_CODE = "filterResultCode";

    private static final String COL_FILTER_RESULT_DIAGNOSTIC = "filterResultDiagnostic";

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

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PAGE_NUMBER)}))
    private Integer numberOfPage;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CHECKSUM)}))
    private String checksum;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_GENERATEDFROM)}))
    private String generatedFrom;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DOC_TAG)}))
    private String docTag;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FILTER_RESULT_CODE)}))
    private String filterResultCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FILTER_RESULT_DIAGNOSTIC)}))
    private String filterResultDiagnostic;
}
