package it.pagopa.pn.paperchannel.middleware.db.entities;


import it.pagopa.pn.paperchannel.utils.Const;
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
public class PnRequestError {

    /* Columns */
    public static final String COL_REQUEST_ID = "requestId";
    public static final String COL_CREATED = "created";
    public static final String COL_PA_ID = "paId";
    public static final String COL_ERROR_TYPE = "error";
    public static final String COL_FLOW_THROW = "flowThrow";
    public static final String COL_AUTHOR = "author";
    public static final String COL_GEOKEY = "geokey";

    /* Indexes */
    public static final String AUTHOR_INDEX = "author-index";


    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_REQUEST_ID)}))
    public String requestId;

    @Getter(onMethod = @__({@DynamoDbSortKey,@DynamoDbSecondarySortKey(indexNames = AUTHOR_INDEX), @DynamoDbAttribute(COL_CREATED)}))
    public Instant created;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = AUTHOR_INDEX), @DynamoDbAttribute(COL_AUTHOR)}))
    public String author;

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

        public PnRequestError build() {
            PnRequestError pnRequestError = new PnRequestError();

            pnRequestError.setRequestId(this.requestId);
            pnRequestError.setPaId(this.paId);
            pnRequestError.setError(this.error);
            pnRequestError.setFlowThrow(this.flowThrow);
            pnRequestError.setGeokey(geokey);

            /* Auto-generated constant field values */
            pnRequestError.setCreated(Instant.now());
            pnRequestError.setAuthor(Const.PN_PAPER_CHANNEL);

            return pnRequestError;
        }

    }
}
