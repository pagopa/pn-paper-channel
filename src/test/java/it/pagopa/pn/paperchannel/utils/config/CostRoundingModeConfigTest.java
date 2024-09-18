package it.pagopa.pn.paperchannel.utils.config;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.math.RoundingMode;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INCORRECT_ROUNDING_MODE;
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
        when(config.getCostRoundingMode()).thenReturn(RoundingMode.HALF_UP.name());

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
        when(config.getCostRoundingMode()).thenReturn(RoundingMode.HALF_DOWN.name());

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
        when(config.getCostRoundingMode()).thenReturn(RoundingMode.UP.name());

        // Then
        var exception = assertThrows(IllegalArgumentException.class, () -> costRoundingModeConfig.setUp());
        assertEquals(INCORRECT_ROUNDING_MODE.getMessage(), exception.getMessage());
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