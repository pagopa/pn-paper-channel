package it.pagopa.pn.paperchannel.middleware.queue.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;


class InternalEventHeaderTest {

    private Integer attempt;
    private Instant expired;
    private String clientId;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        InternalEventHeader internalEventHeader = initInternalEventHeader();
        Assertions.assertNotNull(internalEventHeader);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(internalEventHeader.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("attempt=");
        stringBuilder.append(attempt);
        stringBuilder.append(", ");
        stringBuilder.append("expired=");
        stringBuilder.append(expired);
        stringBuilder.append(", ");
        stringBuilder.append("clientId=");
        stringBuilder.append(clientId);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, internalEventHeader.toString());
    }

    @Test
    void equalsTest() {
        InternalEventHeader internalEventHeaderA = initInternalEventHeader();
        InternalEventHeader internalEventHeaderB = initInternalEventHeader();
        Assertions.assertTrue(internalEventHeaderA.equals(internalEventHeaderB) && internalEventHeaderB.equals(internalEventHeaderA));

        InternalEventHeader internalEventHeaderC = new InternalEventHeader(1, Instant.now());
        Assertions.assertNotEquals(internalEventHeaderA, internalEventHeaderC);
        Assertions.assertNotEquals(internalEventHeaderB, internalEventHeaderC);
    }

    @Test
    void hashCodeTest() {
        InternalEventHeader internalEventHeaderA = initInternalEventHeader();
        InternalEventHeader internalEventHeaderB = initInternalEventHeader();
        Assertions.assertTrue(internalEventHeaderA.equals(internalEventHeaderB) && internalEventHeaderB.equals(internalEventHeaderA));
        Assertions.assertEquals(internalEventHeaderA.hashCode(), internalEventHeaderB.hashCode());
    }

    private InternalEventHeader initInternalEventHeader() {
       return new InternalEventHeader(attempt, expired, clientId);
    }

    private void initialize() {
        attempt = 0;
        expired = null;
        clientId = "ABC";
    }
}
