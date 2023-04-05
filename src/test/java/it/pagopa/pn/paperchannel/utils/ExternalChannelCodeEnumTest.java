package it.pagopa.pn.paperchannel.utils;

import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

class ExternalChannelCodeEnumTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Test
    void isRetryStatusCodeTest() {
        Assertions.assertTrue(ExternalChannelCodeEnum.isRetryStatusCode("RECRS006"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isRetryStatusCode("RECRN006"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isRetryStatusCode("RECAG004"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isRetryStatusCode("RECRI005"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isRetryStatusCode("RECRSI005"));
        Assertions.assertFalse(ExternalChannelCodeEnum.isRetryStatusCode(""));
    }

    @Test
    void isErrorStatusCodeTest() {
        Assertions.assertTrue(ExternalChannelCodeEnum.isErrorStatusCode("CON998"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isErrorStatusCode("CON997"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isErrorStatusCode("CON996"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isErrorStatusCode("CON995"));
        Assertions.assertTrue(ExternalChannelCodeEnum.isErrorStatusCode("CON995"));
        Assertions.assertFalse(ExternalChannelCodeEnum.isErrorStatusCode(""));
    }

    @Test
    void getStatusCodeTest() {
        String message = ExternalChannelCodeEnum.getStatusCode(ExternalChannelCodeEnum.CON080.name());
        Assertions.assertNotNull(message);
        Assertions.assertEquals(ExternalChannelCodeEnum.CON080.getMessage(), message);
    }

    @Test
    void getStatusCodeThrowErrorTest() {
        exception.expect(Exception.class);
        exception.expectMessage("no code found ");
        ExternalChannelCodeEnum.getStatusCode(null);
    }
}
