package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_LISTENER_EXCEPTION;


class PnRequestErrorTest {

    public String requestId;
    public Instant created;
    public String error;
    public String flowThrow;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnRequestError pnRequestError = initRequestError();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnRequestError.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("requestId=");
        stringBuilder.append(requestId);
        stringBuilder.append(", ");
        stringBuilder.append("created=");
        stringBuilder.append(created);
        stringBuilder.append(", ");
        stringBuilder.append("error=");
        stringBuilder.append(error);
        stringBuilder.append(", ");
        stringBuilder.append("flowThrow=");
        stringBuilder.append(flowThrow);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnRequestError.toString());
    }

    private PnRequestError initRequestError() {
        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setRequestId(requestId);
        pnRequestError.setCreated(created);
        pnRequestError.setError(error);
        pnRequestError.setFlowThrow(flowThrow);
        return pnRequestError;
    }

    private void initialize() {
        requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        created = Instant.now();
        error = EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage();
        flowThrow = EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name();
    }
}