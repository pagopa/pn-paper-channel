package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.model.RequestErrorCategoryEnum;
import it.pagopa.pn.paperchannel.model.RequestErrorCauseEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@DynamoDbBean
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PnRequestError {

    /* Columns */
    public static final String COL_REQUEST_ID = "requestId";
    public static final String COL_CREATED = "created";
    public static final String COL_PA_ID = "paId";
    public static final String COL_CATEGORY = "category";
    public static final String COL_CAUSE = "cause";
    public static final String COL_ERROR_TYPE = "error";
    public static final String COL_FLOW_THROW = "flowThrow";
    public static final String COL_AUTHOR = "author";
    public static final String COL_GEOKEY = "geokey";

    /* Indexes */
    public static final String AUTHOR_INDEX = "author-index";
    public static final String CATEGORY_INDEX = "category-index";

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(COL_REQUEST_ID)}))
    public String requestId;

    @Getter(onMethod = @__({@DynamoDbSortKey,
            @DynamoDbSecondarySortKey(indexNames = AUTHOR_INDEX),
            @DynamoDbAttribute(COL_CREATED)}))
    public Instant created;

    @Getter(onMethod = @__({
            @DynamoDbSecondaryPartitionKey(indexNames = AUTHOR_INDEX),
            @DynamoDbAttribute(COL_AUTHOR)}))
    public String author;

    @Getter(onMethod = @__({
            @DynamoDbSecondaryPartitionKey(indexNames = CATEGORY_INDEX),
            @DynamoDbAttribute(COL_CATEGORY)}))
    public String category;

    // Format: <CAUSE>##<TIMESTAMP>
    @Getter(onMethod = @__({
            @DynamoDbSecondarySortKey(indexNames = CATEGORY_INDEX),
            @DynamoDbAttribute(COL_CAUSE)}))
    public String cause;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PA_ID)}))
    public String paId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ERROR_TYPE)}))
    public String error;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FLOW_THROW)}))
    public String flowThrow;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_GEOKEY)}))
    public String geokey;

    public static PnRequestErrorBuilder builder() {
        return new PnRequestErrorBuilder();
    }

    /* Builder class */
    public static class PnRequestErrorBuilder {

        private String requestId;
        private String paId;
        private String error;
        private String flowThrow;
        private String geokey;
        private RequestErrorCategoryEnum category = RequestErrorCategoryEnum.UNKNOWN;
        private RequestErrorCauseEnum cause = RequestErrorCauseEnum.UNKNOWN;

        // Private constructor
        private PnRequestErrorBuilder() {}

        public PnRequestErrorBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public PnRequestErrorBuilder paId(String paId) {
            this.paId = paId;
            return this;
        }

        public PnRequestErrorBuilder error(String error) {
            this.error = error;
            return this;
        }

        public PnRequestErrorBuilder flowThrow(String flowThrow) {
            this.flowThrow = flowThrow;
            return this;
        }

        public PnRequestErrorBuilder geokey(String geokey) {
            this.geokey = geokey;
            return this;
        }

        public PnRequestErrorBuilder category(RequestErrorCategoryEnum category) {
            this.category = category;
            return this;
        }

        public PnRequestErrorBuilder cause(RequestErrorCauseEnum cause) {
            this.cause = cause;
            return this;
        }

        public PnRequestError build() {
            var timestamp = Instant.now();
            PnRequestError pnRequestError = new PnRequestError();

            pnRequestError.setRequestId(this.requestId);
            pnRequestError.setPaId(this.paId);
            pnRequestError.setError(this.error);
            pnRequestError.setFlowThrow(this.flowThrow);
            pnRequestError.setGeokey(this.geokey);
            pnRequestError.setCategory(this.category.getValue());
            pnRequestError.setCause(this.cause.getValue() + "##" + timestamp);

            /* Auto-generated constant field values */
            pnRequestError.setCreated(timestamp);
            pnRequestError.setAuthor(Const.PN_PAPER_CHANNEL);

            return pnRequestError;
        }

    }
}
