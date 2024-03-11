package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
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
    public String author;
    public String paId;

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
        stringBuilder.append("author=");
        stringBuilder.append(author);
        stringBuilder.append(", ");
        stringBuilder.append("paId=");
        stringBuilder.append(paId);
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
        pnRequestError.setAuthor(author);
        pnRequestError.setError(error);
        pnRequestError.setFlowThrow(flowThrow);
        pnRequestError.setPaId(paId);
        return pnRequestError;
    }

    private void initialize() {
        requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        created = Instant.now();
        author= Const.PN_PAPER_CHANNEL;
        error = EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage();
        flowThrow = EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name();
        paId = "0123456789";
    }
}
