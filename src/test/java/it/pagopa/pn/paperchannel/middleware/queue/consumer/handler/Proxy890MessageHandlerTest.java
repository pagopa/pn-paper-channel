package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class Proxy890MessageHandlerTest {

    private Complex890MessageHandler mockComplex890MessageHandler;
    private Proxy890MessageHandler handler;

    private PnPaperChannelConfig pnPaperChannelConfig;

    @BeforeEach
    public void init() {
        mockComplex890MessageHandler = mock(Complex890MessageHandler.class);

        pnPaperChannelConfig = new PnPaperChannelConfig();

        handler = Proxy890MessageHandler.builder()
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .complex890MessageHandler(mockComplex890MessageHandler)
                .build();
    }

    @Test
    void handleMessageOKTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG005C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());


        when(mockComplex890MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che faccia il flusso PNAG012
        verify(mockComplex890MessageHandler, times(1)).handleMessage(entity, paperRequest);

    }
}
