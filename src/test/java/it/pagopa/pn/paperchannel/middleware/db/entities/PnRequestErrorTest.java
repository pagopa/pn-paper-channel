package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.RequestErrorCategoryEnum;
import it.pagopa.pn.paperchannel.model.RequestErrorCauseEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_LISTENER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class PnRequestErrorTest {

    public String requestId;
    public Instant created;
    public String error;
    public String flowThrow;
    public String author;
    public String paId;
    public String geokey;
    public String category;
    public String cause;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnRequestError pnRequestError = initRequestError();

        String toTest = pnRequestError.getClass().getSimpleName() +
                "("+
                "requestId=" + requestId + ", " +
                "created=" + created + ", " +
                "author=" + author + ", " +
                "category=" + category + ", " +
                "cause=" + cause + ", " +
                "paId=" + paId + ", " +
                "error=" + error + ", " +
                "flowThrow=" + flowThrow + ", " +
                "geokey=" + geokey +
                ")";
        assertEquals(toTest, pnRequestError.toString());
    }

    @Test
    void testBuilderCreatesValidEntity() {
        // Act
        PnRequestError requestError = PnRequestError.builder()
                .requestId(requestId)
                .paId(paId)
                .error(error)
                .flowThrow(flowThrow)
                .geokey(geokey)
                .category(RequestErrorCategoryEnum.UNKNOWN)
                .cause(RequestErrorCauseEnum.UNKNOWN)
                .build();

        // Assert
        assertNotNull(error);
        assertEquals(requestId, requestError.getRequestId());
        assertEquals(paId, requestError.getPaId());
        assertEquals(error, requestError.getError());
        assertEquals(flowThrow, requestError.getFlowThrow());
        assertEquals(geokey, requestError.getGeokey());
        assertEquals(RequestErrorCategoryEnum.UNKNOWN.getValue(), requestError.getCategory());
        assertTrue(requestError.getCause().startsWith(RequestErrorCauseEnum.UNKNOWN.getValue() + "##"));
        assertEquals(Const.PN_PAPER_CHANNEL, requestError.getAuthor());
        assertNotNull(requestError.getCreated());
    }

    @Test
    void testTimestampFormatInCauseField() {
        // Arrange
        PnRequestError error = PnRequestError.builder()
                .cause(RequestErrorCauseEnum.UNKNOWN)
                .build();
        String[] causeParts = error.getCause().split("##");

        // Act
        assertEquals(2, causeParts.length);
        assertEquals(RequestErrorCauseEnum.UNKNOWN.getValue(), causeParts[0]);

        // Verify timestamp format
        String timestamp = causeParts[1];
        log.info("ISO_TIMESTAMP={}", timestamp);
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z"),
                "Timestamp should be in ISO-8601 format");
    }

    private PnRequestError initRequestError() {
        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setRequestId(requestId);
        pnRequestError.setCreated(created);
        pnRequestError.setAuthor(author);
        pnRequestError.setError(error);
        pnRequestError.setFlowThrow(flowThrow);
        pnRequestError.setPaId(paId);
        pnRequestError.setGeokey(geokey);
        pnRequestError.setCategory(category);
        pnRequestError.setCause(cause);
        return pnRequestError;
    }

    private void initialize() {
        requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        created = Instant.now();
        author= Const.PN_PAPER_CHANNEL;
        error = EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage();
        flowThrow = EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name();
        paId = "0123456789";
        geokey = "63087";
        category = RequestErrorCategoryEnum.UNKNOWN.getValue();
        cause = RequestErrorCauseEnum.UNKNOWN.getValue() + "##" + Instant.now();
    }
}
