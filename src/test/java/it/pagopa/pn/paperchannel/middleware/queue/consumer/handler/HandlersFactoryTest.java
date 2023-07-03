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
        handlersFactory = new HandlersFactory(null, null, null, mockConfig, null, null, null, null);
        handlersFactory.initializeHandlers();
    }

    @Test
    void getHandlerTest() {
        MessageHandler con080Event = handlersFactory.getHandler("CON080");
        MessageHandler recri001Event = handlersFactory.getHandler("RECRI001");
        MessageHandler recri002Event = handlersFactory.getHandler("RECRI002");
        MessageHandler preEsitoEvent = handlersFactory.getHandler("RECRS002A");
        MessageHandler dematEvent = handlersFactory.getHandler("RECRS002B");
        MessageHandler fascicoloChiuso = handlersFactory.getHandler("RECRS002C");
        MessageHandler retryableErrorEventChiuso = handlersFactory.getHandler("RECRS006");
        MessageHandler notRetryableErrorEventChiuso = handlersFactory.getHandler("CON998");
        MessageHandler unknownEvent = handlersFactory.getHandler("UNKNOWN");
        MessageHandler recag012 = handlersFactory.getHandler("RECAG012");
        MessageHandler recag011B = handlersFactory.getHandler("RECAG011B");
        MessageHandler recag005CEvent = handlersFactory.getHandler("RECAG005C");
        MessageHandler recag006C = handlersFactory.getHandler("RECAG006C");
        MessageHandler recag007C = handlersFactory.getHandler("RECAG007C");

        assertThat(con080Event).isInstanceOf(DirectlySendMessageHandler.class);
        assertThat(recri001Event).isInstanceOf(DirectlySendMessageHandler.class);
        assertThat(recri002Event).isInstanceOf(DirectlySendMessageHandler.class);
        assertThat(preEsitoEvent).isInstanceOf(SaveMetadataMessageHandler.class);
        assertThat(dematEvent).isInstanceOf(SaveDematMessageHandler.class);
        assertThat(fascicoloChiuso).isInstanceOf(AggregatorMessageHandler.class);
        assertThat(retryableErrorEventChiuso).isInstanceOf(RetryableErrorMessageHandler.class);
        assertThat(notRetryableErrorEventChiuso).isInstanceOf(NotRetryableErrorMessageHandler.class);
        assertThat(unknownEvent).isInstanceOf(LogMessageHandler.class);
        assertThat(recag012).isInstanceOf(RECAG012MessageHandler.class);
        assertThat(recag011B).isInstanceOf(RECAG011BMessageHandler.class);
        assertThat(recag005CEvent)
                .isInstanceOf(Complex890MessageHandler.class)
                .isEqualTo(recag006C)
                .isEqualTo(recag007C);
    }

}
