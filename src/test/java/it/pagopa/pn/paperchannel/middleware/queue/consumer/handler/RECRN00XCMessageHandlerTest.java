package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN00XCAbstractMessageHandler;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RECRN00XCAbstractMessageHandlerTest {

    @SuperBuilder
    static class RECRN00XCAbstractMessageHandlerImpl extends RECRN00XCAbstractMessageHandler {}

    @Mock
    private SqsSender sqsSender;
    @Mock
    private EventMetaDAO eventMetaDAO;
    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    private MetaDematCleaner metaDematCleaner;

    private PnPaperChannelConfig pnPaperChannelConfig;
    private RECRN00XCAbstractMessageHandlerImpl handler;

    @BeforeEach
    void setUp(){
        pnPaperChannelConfig = new PnPaperChannelConfig();
        pnPaperChannelConfig.setRefinementDuration(Duration.ofDays(10));
        pnPaperChannelConfig.setCompiutaGiacenzaArDuration(Duration.ofDays(30));
        pnPaperChannelConfig.setEnableTruncatedDateForRefinementCheck(false);

        handler = RECRN00XCAbstractMessageHandlerImpl.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
    }

    @Test
    void testGetDurationBetweenDates_NoTruncate() {
        // Arrange
        var instant1 = Instant.parse("2025-03-05T09:00:00Z");
        var instant2 = Instant.parse("2025-03-06T08:00:00Z");

        // Act
        Duration duration = ReflectionTestUtils.invokeMethod(
                handler,
                "getDurationBetweenDates",
                instant1,
                instant2
        );

        // Assert
        assertThat(duration).isEqualTo(Duration.ofHours(23));
    }

    @Test
    void testGetDurationBetweenDates_Truncate() {
        // Arrange
        pnPaperChannelConfig.setEnableTruncatedDateForRefinementCheck(true);
        var instant1 = Instant.parse("2025-03-05T09:00:00Z");
        var instant2 = Instant.parse("2025-03-06T08:00:00Z");

        // Act
        Duration duration = ReflectionTestUtils.invokeMethod(
                handler,
                "getDurationBetweenDates",
                instant1,
                instant2
        );

        // Assert
        assertThat(duration).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void testIsDifferenceGreaterOrEqualToRefinementDuration() {
        // Arrange
        Instant now = Instant.now();
        Instant nineDaysAfter = now.plus(Duration.ofDays(9));
        Instant tenDaysAfter = now.plus(Duration.ofDays(10));
        Instant elevenDaysAfter = now.plus(Duration.ofDays(11));

        // Act
        Boolean result9 = ReflectionTestUtils.invokeMethod(
                handler,
                "isDifferenceGreaterOrEqualToRefinementDuration",
                now,
                nineDaysAfter
        );
        Boolean result10 = ReflectionTestUtils.invokeMethod(
                handler,
                "isDifferenceGreaterOrEqualToRefinementDuration",
                now,
                tenDaysAfter
        );
        Boolean result11 = ReflectionTestUtils.invokeMethod(
                handler,
                "isDifferenceGreaterOrEqualToRefinementDuration",
                now,
                elevenDaysAfter
        );

        // Assert
        assertThat(result9).isFalse();
        assertThat(result10).isTrue();
        assertThat(result11).isTrue();
    }

    @Test
    void testIsDifferenceGreaterOrEqualToStockDuration() {
        // Arrange
        Instant now = Instant.now();
        Instant twentyNineDays = now.plus(Duration.ofDays(29));
        Instant thirtyDays = now.plus(Duration.ofDays(30));
        Instant thirtyOneDays = now.plus(Duration.ofDays(31));

        // Act
        Boolean result29 = ReflectionTestUtils.invokeMethod(
                handler,
                "isDifferenceGreaterOrEqualToStockDuration",
                now,
                twentyNineDays
        );
        Boolean result30 = ReflectionTestUtils.invokeMethod(
                handler,
                "isDifferenceGreaterOrEqualToStockDuration",
                now,
                thirtyDays
        );
        Boolean result31 = ReflectionTestUtils.invokeMethod(
                handler,
                "isDifferenceGreaterOrEqualToStockDuration",
                now,
                thirtyOneDays
        );

        // Assert
        assertThat(result29).isFalse();
        assertThat(result30).isTrue();
        assertThat(result31).isTrue();
    }

    @Test
    void testTruncateToStartOfDay() {
        // Arrange
        Instant instant = Instant.parse("2023-04-12T13:45:10Z");

        // Act
        Instant truncated = ReflectionTestUtils.invokeMethod(
                handler,
                "truncateToStartOfDay",
                instant
        );

        // Assert
        // Data "attesa": "2023-04-12T00:00:00Z"
        Instant expected = LocalDate.of(2023, 4, 12)
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant();

        assertThat(truncated).isEqualTo(expected);
    }
}
