package it.pagopa.pn.paperchannel.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PnDematNotValidExceptionTest {

    @Test
    void testException() {
        assertDoesNotThrow(() -> new PnDematNotValidException("message"));
    }
}
