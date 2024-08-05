package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SendProgressMetaConfigTest {

    private PnPaperChannelConfig config;
    private SendProgressMetaConfig sendProgressMetaConfig;

    @BeforeEach
    void setUp() {
        config = Mockito.mock(PnPaperChannelConfig.class);
        sendProgressMetaConfig = new SendProgressMetaConfig(config);
    }

    @Test
    void testValidConfiguration() {
        // Given
        // When
        when(config.getSendProgressMeta()).thenReturn(List.of("META", "RECAG012"));

        // Then
        assertDoesNotThrow(() -> sendProgressMetaConfig.validateConfiguration());
        assertTrue(sendProgressMetaConfig.isMetaEnabled());
        assertTrue(sendProgressMetaConfig.isRECAG012Enabled());
        assertFalse(sendProgressMetaConfig.isCCON018Enabled());
        assertFalse(sendProgressMetaConfig.isDisabled());
    }

    @Test
    void testInvalidConfigurationValue() {
        // Given
        // When
        when(config.getSendProgressMeta()).thenReturn(List.of("INVALID"));

        // Then
        assertThrows(IllegalStateException.class, () -> sendProgressMetaConfig.validateConfiguration());
    }

    @Test
    void testDisableWithOtherValues() {
        // Given
        // When
        when(config.getSendProgressMeta()).thenReturn(List.of("DISABLE", "META"));

        // Then
        assertThrows(IllegalStateException.class, () -> sendProgressMetaConfig.validateConfiguration());
    }

    @Test
    void testDisableOnly() {
        // Given
        // When
        when(config.getSendProgressMeta()).thenReturn(List.of("DISABLE"));

        // Then
        assertDoesNotThrow(() -> sendProgressMetaConfig.validateConfiguration());
        assertFalse(sendProgressMetaConfig.isMetaEnabled());
        assertFalse(sendProgressMetaConfig.isRECAG012Enabled());
        assertFalse(sendProgressMetaConfig.isCCON018Enabled());
        assertTrue(sendProgressMetaConfig.isDisabled());
    }

    @Test
    void testEmptyConfiguration() {
        // Given
        // When
        when(config.getSendProgressMeta()).thenReturn(List.of());

        // Then
        assertDoesNotThrow(() -> sendProgressMetaConfig.validateConfiguration());
        assertFalse(sendProgressMetaConfig.isMetaEnabled());
        assertFalse(sendProgressMetaConfig.isRECAG012Enabled());
        assertFalse(sendProgressMetaConfig.isCCON018Enabled());
        assertTrue(sendProgressMetaConfig.isDisabled());
    }

    @Test
    void testNullConfiguration() {
        // Given
        // When
        when(config.getSendProgressMeta()).thenReturn(null);

        // Then
        assertDoesNotThrow(() -> sendProgressMetaConfig.validateConfiguration());
        assertFalse(sendProgressMetaConfig.isMetaEnabled());
        assertFalse(sendProgressMetaConfig.isRECAG012Enabled());
        assertFalse(sendProgressMetaConfig.isCCON018Enabled());
        assertTrue(sendProgressMetaConfig.isDisabled());
    }
}