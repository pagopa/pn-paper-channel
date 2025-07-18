package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN004CMessageHandler;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN005CMessageHandler;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN003CMessageHandler;
import it.pagopa.pn.paperchannel.utils.SendProgressMetaConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlersFactoryTest {

    private HandlersFactory handlersFactory;

    private final PnPaperChannelConfig mockConfig = mock(PnPaperChannelConfig.class);
    private final SendProgressMetaConfig mockSendProgressMetaConfig = mock(SendProgressMetaConfig.class);

    enum FeatureFlag {
        SIMPLE_890_FLOW,
        SEND_PROGRESS_META,
        SEND_PROGRESS_META_CON018,
        SEND_PROGRESS_META_RECAG012A
    }
    record FFTestCases(EnumSet<FeatureFlag> enabledFeatureFlags, List<TestCase> testCases) {}
    record TestCase(String name, List<String> codes, Class<? extends MessageHandler> handlerClass) {}

    @ParameterizedTest
    @MethodSource("getTestCases")
    void testHandler(EnumSet<FeatureFlag> featureFlags, List<String> statusCodes,
                         Class<? extends MessageHandler> expectedHandlerClass) {
        // Given
        handlersFactory = new HandlersFactory(null, null, null,
                mockConfig, null, null, null, null,
                null, null, mockSendProgressMetaConfig,
                null, null, null);

        // When
        when(mockConfig.isEnableSimple890Flow())
                .thenReturn(featureFlags.contains(FeatureFlag.SIMPLE_890_FLOW));
        when(mockConfig.isEnableRetryCon996())
                .thenReturn(Boolean.TRUE);
        when(mockConfig.isSendCon020())
                .thenReturn(true);
        when(mockSendProgressMetaConfig.isMetaEnabled())
                .thenReturn(featureFlags.contains(FeatureFlag.SEND_PROGRESS_META));
        when(mockSendProgressMetaConfig.isCON018Enabled())
                .thenReturn(featureFlags.contains(FeatureFlag.SEND_PROGRESS_META_CON018));
        when(mockSendProgressMetaConfig.isRECAG012AEnabled())
                .thenReturn(featureFlags.contains(FeatureFlag.SEND_PROGRESS_META_RECAG012A));

        handlersFactory.initializeHandlers();

        // Then
        assertThat(statusCodes)
                .hasSizeGreaterThan(0)
                .allMatch(statusCode -> expectedHandlerClass.isInstance(handlersFactory.getHandler(statusCode)));
    }

    /**
     * Generates a stream of test cases for different handler scenarios based on feature flags.
     *
     * @return A Stream of Arguments
     */
    private static Stream<Arguments> getTestCases() {
        var cases = Stream.of(
            // Common cases
            new FFTestCases(EnumSet.noneOf(FeatureFlag.class),
                    List.of(
                        new TestCase("SendToDeliveryPush",
                                List.of("CON080", "RECRI001", "RECRI002", "RECRS001C", "RECRS003C",
                                        "RECRS015", "RECRN015", "RECAG015", "RECAG010", "RECRS010"),
                                SendToDeliveryPushHandler.class),
                        new TestCase("CON996", List.of("CON996"), ProxyCON996MessageHandler.class),
                        new TestCase("SaveDemat", List.of("RECRS002B"), SaveDematMessageHandler.class),
                        new TestCase("Aggregator", List.of("RECRS002C"), AggregatorMessageHandler.class),
                        new TestCase("Retryable", List.of("RECRS006"), RetryableErrorMessageHandler.class),
                        new TestCase("NotRetryable", List.of("CON998"), NotRetryableErrorMessageHandler.class),
                        new TestCase("Log", List.of("UNKNOWN"), LogMessageHandler.class),
                        new TestCase("Complex890", List.of("RECAG005C", "RECAG006C", "RECAG007C", "RECAG008C"),
                                Proxy890MessageHandler.class),
                        new TestCase("RECRN003C", List.of("RECRN003C"), SendToOcrProxyHandler.class),
                        new TestCase("RECRN004C", List.of("RECRN004C"), SendToOcrProxyHandler.class),
                        new TestCase("RECRN005C", List.of("RECRN005C"), SendToOcrProxyHandler.class)
                    )),
            // SIMPLE_890_FLOW ENABLE cases
            new FFTestCases(
                    EnumSet.of(FeatureFlag.SIMPLE_890_FLOW),
                    List.of(
                            new TestCase("RECAG012", List.of("RECAG012"), ChainedMessageHandler.class),
                            new TestCase("RECAG011B", List.of("RECAG011B"), ChainedMessageHandler.class),
                            new TestCase("RECAG007B", List.of("RECAG007B"), ChainedMessageHandler.class)
                    )),
            // SIMPLE_890_FLOW DISABLE cases
            new FFTestCases(
                    EnumSet.noneOf(FeatureFlag.class),
                    List.of(
                            new TestCase("RECAG012", List.of("RECAG012"), ChainedMessageHandler.class),
                            new TestCase("RECAG011B", List.of("RECAG011B"), RECAG011BMessageHandler.class),
                            new TestCase("RECAG007B", List.of("RECAG007B"), SaveDematMessageHandler.class)
                    )),
            // SEND_PROGRESS_META ENABLE cases
            new FFTestCases(
                    EnumSet.of(FeatureFlag.SEND_PROGRESS_META),
                    List.of(
                            new TestCase("SaveMetadata",
                                    List.of("RECRS002A", "RECRS002D", "RECRN001A", "RECRN002A", "RECRN002D",
                                            "RECAG001A", "RECAG002A", "RECAG003A", "RECAG003D", "RECRS004A", "RECRS005A",
                                            "RECRN003A", "RECRN004A", "RECRN005A", "RECAG005A", "RECAG006A", "RECAG007A",
                                            "RECAG008A", "RECRSI004A", "RECRI003A", "RECRI004A", "RECRN010"),
                                    ChainedMessageHandler.class)
                    )),
            // SEND_PROGRESS_META DISABLE cases
            new FFTestCases(
                    EnumSet.noneOf(FeatureFlag.class),
                    List.of(
                            new TestCase("SaveMetadata",
                                    List.of("RECRS002A", "RECRS002D", "RECRN001A", "RECRN002A", "RECRN002D",
                                            "RECAG001A","RECAG002A", "RECAG003A", "RECAG003D", "RECRS004A", "RECRS005A",
                                            "RECRN003A", "RECRN004A", "RECRN005A", "RECAG005A", "RECAG006A", "RECAG007A",
                                            "RECAG008A", "RECRSI004A", "RECRI003A", "RECRI004A"),
                                    ChainedMessageHandler.class)
                    )),
            // SEND_PROGRESS_META SEND_PROGRESS_META_CCON018 ENABLE cases
            new FFTestCases(
                    EnumSet.of(FeatureFlag.SEND_PROGRESS_META, FeatureFlag.SEND_PROGRESS_META_CON018),
                    List.of(
                            new TestCase("CON018", List.of("CON018"), SendToDeliveryPushHandler.class)
                    )),
            // SEND_PROGRESS_META SEND_PROGRESS_META_RECAG012A ENABLE cases
            new FFTestCases(
                    EnumSet.of(
                            FeatureFlag.SIMPLE_890_FLOW,
                            FeatureFlag.SEND_PROGRESS_META,
                            FeatureFlag.SEND_PROGRESS_META_RECAG012A),
                    List.of(
                            new TestCase("RECAG012A", List.of("RECAG012"), ChainedMessageHandler.class)
                    )),
            new FFTestCases(
                    EnumSet.of(
                            FeatureFlag.SEND_PROGRESS_META,
                            FeatureFlag.SEND_PROGRESS_META_RECAG012A),
                    List.of(
                            new TestCase("RECAG012A", List.of("RECAG012"), ChainedMessageHandler.class)
                    ))
        );

        return cases.flatMap(ffTestCases -> ffTestCases.testCases().stream()
                .map(testCase ->
                        Arguments.of(ffTestCases.enabledFeatureFlags, testCase.codes(), testCase.handlerClass())));
    }

}
