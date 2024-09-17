package it.pagopa.pn.paperchannel.utils.config;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.math.RoundingMode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


public class CostRoundingModeConfigTest {
    private PnPaperChannelConfig config;
    private CostRoundingModeConfig costRoundingModeConfig;


    @BeforeEach
    void setUp() {
        config = Mockito.mock(PnPaperChannelConfig.class);
        costRoundingModeConfig = new CostRoundingModeConfig(config);
    }

    @Test
    void testRoundingModeHalfUp() {
        // Given
        // When
        when(config.getCostRoundingMode()).thenReturn("HALF_UP");

        // Then
        assertDoesNotThrow(() -> {
            costRoundingModeConfig.setUp();
            RoundingMode roundingMode = costRoundingModeConfig.getRoundingMode();
            assertNotNull(roundingMode);
            assertEquals(RoundingMode.HALF_UP, roundingMode);
        });
    }

    @Test
    void testRoundingModeHalfDown() {
        // Given
        // When
        when(config.getCostRoundingMode()).thenReturn("HALF_DOWN");

        // Then
        assertDoesNotThrow(() -> {
            costRoundingModeConfig.setUp();
            RoundingMode roundingMode = costRoundingModeConfig.getRoundingMode();
            assertNotNull(roundingMode);
            assertEquals(RoundingMode.HALF_DOWN, roundingMode);
        });
    }

    @Test
    void testRoundingModeWrongValueThenThrow() {
        // Given
        // When
        when(config.getCostRoundingMode()).thenReturn("ILLEGAL_ARGUMENT_TEST");

        // Then
        assertThrows(IllegalArgumentException.class,() -> costRoundingModeConfig.setUp());
    }

    @Test
    void testRoundingModeEmptyValueThenThrow() {
        // Given
        // When
        when(config.getCostRoundingMode()).thenReturn("");

        // Then
        assertThrows(IllegalArgumentException.class,() -> costRoundingModeConfig.setUp());
    }

    @Test
    void testRoundingModeNullValueThenThrow() {
        // Given
        // When
        when(config.getCostRoundingMode()).thenReturn(null);

        // Then
        assertThrows(NullPointerException.class,() -> costRoundingModeConfig.setUp());
    }
}