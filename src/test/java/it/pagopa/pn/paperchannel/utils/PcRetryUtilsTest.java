package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PcRetryUtilsTest {

    private PcRetryUtils pcRetryUtils;
    private PnPaperChannelConfig config;

    @BeforeEach
    void setup() {
        config = mock(PnPaperChannelConfig.class);
        pcRetryUtils = new PcRetryUtils(config);
    }


    @Test
    void testHasOtherAttempt_True() {
        String requestId = "ABC_RETRY_1";
        when(config.getMaxPcRetry()).thenReturn(5);
        Assertions.assertTrue(pcRetryUtils.hasOtherAttempt(requestId));
    }

    @Test
    void testHasOtherAttempt_False() {
        String requestId = "ABC_.PCRETRY_3";
        when(config.getMaxPcRetry()).thenReturn(2);
        Assertions.assertFalse(pcRetryUtils.hasOtherAttempt(requestId));
    }

    @Test
    void testSetRetryRequestId_WhenRetryPresent() {
        String requestId = "XYZ_.PCRETRY_2";
        String result = pcRetryUtils.setRetryRequestId(requestId);
        Assertions.assertEquals("XYZ_.PCRETRY_3", result);
    }

    @Test
    void testSetRetryRequestId_WhenRetryNotPresent() {
        String requestId = "XYZ";
        String result = pcRetryUtils.setRetryRequestId(requestId);
        Assertions.assertEquals("XYZ", result);
    }

    @Test
    void testCheckHasOtherAttemptAndMapPcRetryResponse_WithRetry() {
        when(config.getMaxPcRetry()).thenReturn(10);
        PcRetryResponse response = pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse("ABC_.PCRETRY_2", "driver-1");

        Assertions.assertEquals("ABC_.PCRETRY_2", response.getParentRequestId());
        Assertions.assertEquals("driver-1", response.getDeliveryDriverId());
        Assertions.assertTrue(response.getRetryFound());
        Assertions.assertEquals("PCRETRY_3", response.getPcRetry());
        Assertions.assertEquals("ABC_.PCRETRY_3", response.getRequestId());
    }

    @Test
    void testCheckHasOtherAttemptAndMapPcRetryResponse_WithoutRetry() {
        when(config.getMaxPcRetry()).thenReturn(1);
        PcRetryResponse response = pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse("ABC_.PCRETRY_2", "driver-1");

        Assertions.assertEquals("ABC_.PCRETRY_2", response.getParentRequestId());
        Assertions.assertEquals("driver-1", response.getDeliveryDriverId());
        Assertions.assertFalse(response.getRetryFound());
        Assertions.assertNull(response.getPcRetry());
        Assertions.assertNull(response.getRequestId());
    }
}
