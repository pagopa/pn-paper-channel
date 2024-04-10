package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HandlersFactoryTest {

    private HandlersFactory handlersFactory;

    @BeforeEach
    public void init() {
        PnPaperChannelConfig mockConfig = mock(PnPaperChannelConfig.class);
        handlersFactory = new HandlersFactory(null, null, null, mockConfig, null, null, null, null, null, null);
        handlersFactory.initializeHandlers();
    }

    @ParameterizedTest
    @MethodSource(value = "getHandlerTestCases")
    void getHandlerTest(List<String> progressStatusEventCodes, Class<? extends MessageHandler> clazz) {

        assertThat(progressStatusEventCodes)
                .hasSizeGreaterThan(0)
                .allMatch(statusCode -> clazz.isInstance(handlersFactory.getHandler(statusCode)));
    }

    /**
     * Build test argument cases for {@link HandlersFactoryTest#getHandlerTest}
     * */
    private static Stream<Arguments> getHandlerTestCases() {

        /* Test inputs */
        List<String> directlySendMessageCases = List.of(
                "CON080", "RECRI001", "RECRI002", "RECRS001C", "RECRS003C",
                "RECRS015", "RECRN015", "RECAG015", "RECAG010", "RECRS010", "RECRN010"
        );

        List<String> saveMetadataMessageCases = List.of("RECRS002A");
        List<String> saveDematMessageCases = List.of("RECRS002B");
        List<String> aggregatorMessageCases = List.of("RECRS002C");
        List<String> retryableMessageCases = List.of("RECRS006");
        List<String> notRetryableMessageCases = List.of("CON998");
        List<String> logMessageCases = List.of("UNKNOWN");
        List<String> recag012MessageCases = List.of("RECAG012");
        List<String> recag011bMessageCases = List.of("RECAG011B");
        List<String> recag007bMessageCases = List.of("RECAG007B");
        List<String> complex890MessageCases = List.of("RECAG005C", "RECAG006C", "RECAG007C", "RECAG008C");

        /* Test method arguments */
        Arguments directlySendMessageCasesArguments = Arguments.of(directlySendMessageCases, DirectlySendMessageHandler.class);
        Arguments saveMetadataMessageCasesArguments = Arguments.of(saveMetadataMessageCases, SaveMetadataMessageHandler.class);
        Arguments saveDematMessageCasesArguments = Arguments.of(saveDematMessageCases, SaveDematMessageHandler.class);
        Arguments aggregatorMessageCasesArguments = Arguments.of(aggregatorMessageCases, AggregatorMessageHandler.class);
        Arguments retryableMessageCasesArguments = Arguments.of(retryableMessageCases, RetryableErrorMessageHandler.class);
        Arguments notRetryableMessageCasesArguments = Arguments.of(notRetryableMessageCases, NotRetryableErrorMessageHandler.class);
        Arguments logMessageCasesArguments = Arguments.of(logMessageCases, LogMessageHandler.class);
        Arguments recag012MessageCasesArguments = Arguments.of(recag012MessageCases, RECAG012MessageHandler.class);
        Arguments recag011bMessageCasesArguments = Arguments.of(recag011bMessageCases, ChainedMessageHandler.class);
        Arguments recag007bMessageCasesArguments = Arguments.of(recag007bMessageCases, ChainedMessageHandler.class);
        Arguments complex890MessageCasesArguments = Arguments.of(complex890MessageCases, Proxy890MessageHandler.class);

        return Stream.of(
                directlySendMessageCasesArguments,
                saveMetadataMessageCasesArguments,
                saveDematMessageCasesArguments,
                aggregatorMessageCasesArguments,
                retryableMessageCasesArguments,
                notRetryableMessageCasesArguments,
                logMessageCasesArguments,
                recag012MessageCasesArguments,
                recag011bMessageCasesArguments,
                recag007bMessageCasesArguments,
                complex890MessageCasesArguments
        );
    }
}
