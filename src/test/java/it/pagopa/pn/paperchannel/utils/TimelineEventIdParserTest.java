package it.pagopa.pn.paperchannel.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimelineEventIdParserTest {

    @Test
    void toComponentsReturnsAllFieldsCorrectly() {
        TimelineEventIdParser parser = TimelineEventIdParser.parse("CATEGORY.IUN_12345.PCRETRY_0.RECINDEX_1.ATTEMPT_2");
        TimelineEventIdParser.TimelineEventIdComponents components = parser.toComponents();
        assertEquals("CATEGORY", components.category());
        assertEquals("12345", components.iun());
        assertEquals(0, components.pcRetry());
        assertEquals(1, components.recIndex());
        assertEquals(2, components.sentAttemptMade());
        assertTrue(components.hasCategory());
        assertTrue(components.hasIun());
        assertTrue(components.hasRecIndex());
        assertTrue(components.hasSentAttemptMade());
    }

    @Test
    void toComponentsWithMissingFieldsReturnsNullsAndHasMethodsReturnFalse() {
        TimelineEventIdParser parser = TimelineEventIdParser.parse("CATEGORY");
        TimelineEventIdParser.TimelineEventIdComponents components = parser.toComponents();
        assertEquals("CATEGORY", components.category());
        assertNull(components.iun());
        assertNull(components.recIndex());
        assertNull(components.sentAttemptMade());
        assertNull(components.reworkIndex());
        assertTrue(components.hasCategory());
        assertFalse(components.hasIun());
        assertFalse(components.hasRecIndex());
        assertFalse(components.hasSentAttemptMade());
        assertFalse(components.hasReworkIndex());
    }

    @Test
    void toComponentsWithEmptyEventIdReturnsAllNullsAndHasMethodsReturnFalse() {
        TimelineEventIdParser parser = TimelineEventIdParser.parse("");
        TimelineEventIdParser.TimelineEventIdComponents components = parser.toComponents();
        assertNull(components.category());
        assertNull(components.iun());
        assertNull(components.recIndex());
        assertNull(components.sentAttemptMade());
        assertFalse(components.hasCategory());
        assertFalse(components.hasIun());
        assertFalse(components.hasRecIndex());
        assertFalse(components.hasSentAttemptMade());
    }
}