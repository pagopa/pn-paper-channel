package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.utils.SendProgressMetaConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlersFactoryTest {

    private HandlersFactory handlersFactory;

    private final PnPaperChannelConfig mockConfig = mock(PnPaperChannelConfig.class);
    private final SendProgressMetaConfig mockSendProgressMetaConfig = mock(SendProgressMetaConfig.class);

    @ParameterizedTest
    @MethodSource("getHandlerTestCases")
    void getHandlerTest(boolean enableSimple890Flow, boolean enableProgressMeta, List<String> statusCodes, Class<? extends MessageHandler> expectedHandlerClass) {
        // Given
        handlersFactory = new HandlersFactory(null, null, null,
                mockConfig, null, null, null, null,
                null, null, mockSendProgressMetaConfig);

        // When
        when(mockConfig.isEnableSimple890Flow()).thenReturn(enableSimple890Flow);
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(enableProgressMeta);

        handlersFactory.initializeHandlers();

        // Then
        assertThat(statusCodes)
                .hasSizeGreaterThan(0)
                .allMatch(statusCode -> expectedHandlerClass.isInstance(handlersFactory.getHandler(statusCode)));
    }

    /**
     * Generates a stream of test cases for different handler scenarios based on all feature flags combinations.
     */
    private static Stream<Arguments> getHandlerTestCases() {
        return Stream.of(
                getTestCases(true, true),
                getTestCases(true, false),
                getTestCases(false, true),
                getTestCases(false, false)
        ).flatMap(stream -> stream);
    }

    /**
     * Generates a stream of test cases for different handler scenarios based on feature flags.
     *
     * @param enableSimple890Flow A boolean flag indicating whether the Simple890 flow is enabled.
     * @param enableProgressMeta A boolean flag indicating whether send progress metadata is enabled.
     * @return A Stream of Arguments
     */
    private static Stream<Arguments> getTestCases(boolean enableSimple890Flow, boolean enableProgressMeta) {
        record TestCase(String name, List<String> codes, Class<? extends MessageHandler> handlerClass) {}

        List<TestCase> commonCases = List.of(
                new TestCase("SendToDeliveryPush",
                        List.of("CON080", "RECRI001", "RECRI002", "RECRS001C", "RECRS003C",
                            "RECRS015", "RECRN015", "RECAG015", "RECAG010", "RECRS010", "RECRN010"),
                        SendToDeliveryPushHandler.class),
                new TestCase("SaveDemat", List.of("RECRS002B"), SaveDematMessageHandler.class),
                new TestCase("Aggregator", List.of("RECRS002C"), AggregatorMessageHandler.class),
                new TestCase("Retryable", List.of("RECRS006"), RetryableErrorMessageHandler.class),
                new TestCase("NotRetryable", List.of("CON998"), NotRetryableErrorMessageHandler.class),
                new TestCase("Log", List.of("UNKNOWN"), LogMessageHandler.class),
                new TestCase("Complex890", List.of("RECAG005C", "RECAG006C", "RECAG007C", "RECAG008C"),
                        Proxy890MessageHandler.class)
        );

        List<TestCase> simple890DependentCases = List.of(
                new TestCase("RECAG012", List.of("RECAG012"),
                        enableSimple890Flow ? ChainedMessageHandler.class : OldRECAG012MessageHandler.class),
                new TestCase("RECAG011B", List.of("RECAG011B"),
                        enableSimple890Flow ? ChainedMessageHandler.class : RECAG011BMessageHandler.class),
                new TestCase("RECAG007B", List.of("RECAG007B"),
                        enableSimple890Flow ? ChainedMessageHandler.class : SaveDematMessageHandler.class)
        );

        List<TestCase> progressMetaDependentCases = List.of(
                new TestCase("SaveMetadata",
                        List.of("RECRS002A", "RECRS002D", "RECRN001A", "RECRN002A", "RECRN002D", "RECAG001A",
                                "RECAG002A", "RECAG003A", "RECAG003D", "RECRS004A", "RECRS005A", "RECRN003A",
                                "RECRN004A", "RECRN005A", "RECAG005A", "RECAG006A", "RECAG007A", "RECAG008A",
                                "RECRSI004A", "RECRI003A", "RECRI004A"),
                        enableProgressMeta ? ChainedMessageHandler.class : SaveMetadataMessageHandler.class)
        );

        return Stream.of(
                        commonCases.stream(),
                        simple890DependentCases.stream(),
                        progressMetaDependentCases.stream())
                .flatMap(s -> s)
                .map(testCase -> Arguments.of(enableSimple890Flow, enableProgressMeta,
                        testCase.codes, testCase.handlerClass));
    }
}
