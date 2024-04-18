package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LogMessageHandlerTest {


    private LogMessageHandler handler;


    @BeforeEach
    public void init() {
        handler = LogMessageHandler.builder().build();
    }

    @Test
    void handleMessage() {
        assertDoesNotThrow(() -> handler.handleMessage(new PnDeliveryRequest(), new PaperProgressStatusEventDto()).block());
    }

}

