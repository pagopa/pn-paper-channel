package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HandlersFactoryTest {

    private HandlersFactory handlersFactory;

    @BeforeEach
    public void init() {
        PnPaperChannelConfig mockConfig = mock(PnPaperChannelConfig.class);
        handlersFactory = new HandlersFactory(null, null, null, mockConfig, null, null, null);
        handlersFactory.initializeHandlers();
    }

    @Test
    void getHandlerTest() {
        MessageHandler preEsitoEvent = handlersFactory.getHandler("RECRS002A");
        MessageHandler dematEvent = handlersFactory.getHandler("RECRS002B");
        MessageHandler fascicoloChiuso = handlersFactory.getHandler("RECRS002C");
        MessageHandler retryableErrorEventChiuso = handlersFactory.getHandler("RECRS006");
        MessageHandler notRetryableErrorEventChiuso = handlersFactory.getHandler("CON998");
        MessageHandler unknownEvent = handlersFactory.getHandler("UNKNOWN");

        assertThat(preEsitoEvent).isInstanceOf(SaveMetadataMessageHandler.class);
        assertThat(dematEvent).isInstanceOf(SaveDematMessageHandler.class);
        assertThat(fascicoloChiuso).isInstanceOf(AggregatorMessageHandler.class);
        assertThat(retryableErrorEventChiuso).isInstanceOf(RetryableErrorMessageHandler.class);
        assertThat(notRetryableErrorEventChiuso).isInstanceOf(NotRetryableErrorMessageHandler.class);
        assertThat(unknownEvent).isInstanceOf(LogMessageHandler.class);
    }

}
