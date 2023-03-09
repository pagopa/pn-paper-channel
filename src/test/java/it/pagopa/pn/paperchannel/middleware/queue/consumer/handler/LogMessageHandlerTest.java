package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LogMessageHandlerTest {


    private LogMessageHandler handler;


    @BeforeEach
    public void init() {
        handler = new LogMessageHandler();
    }

    @Test
    void handleMessage() {
        assertDoesNotThrow(() -> handler.handleMessage(new PnDeliveryRequest(), new PaperProgressStatusEventDto()));
    }

}

