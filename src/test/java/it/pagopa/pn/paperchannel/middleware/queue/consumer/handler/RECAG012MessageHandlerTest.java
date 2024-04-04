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

class RECAG012MessageHandlerTest {

    private PNAG012MessageHandler mockPnag012MessageHandler;
    private SaveMetadataMessageHandler handler;

    private EventMetaDAO mockDao;

    @BeforeEach
    public void init() {
        long ttlDays = 365;

        mockDao = mock(EventMetaDAO.class);
        mockPnag012MessageHandler = mock(PNAG012MessageHandler.class);

        PnPaperChannelConfig mockConfig = new PnPaperChannelConfig();
        mockConfig.setTtlExecutionDaysMeta(ttlDays);

        handler = RECAG012MessageHandler.builder()
                .eventMetaDAO(mockDao)
                .pnPaperChannelConfig(mockConfig)
                .pnag012MessageHandler(mockPnag012MessageHandler)
                .build();
    }

    @Test
    void handleMessageOKTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG012")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PnEventMeta pnEventMeta = handler.buildPnEventMeta(paperRequest);

        when(mockDao.getDeliveryEventMeta("META##requestId", "META##RECAG012")).thenReturn(Mono.empty());
        when(mockDao.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));
        when(mockPnag012MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento
        verify(mockDao, times(1)).createOrUpdate(pnEventMeta);

        //mi aspetto che faccia il flusso PNAG012
        verify(mockPnag012MessageHandler, times(1)).handleMessage(entity, paperRequest);

    }

    @Test
    void handleMessageKOTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG012")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PnEventMeta pnEventMeta = handler.buildPnEventMeta(paperRequest);

        when(mockDao.getDeliveryEventMeta("META##requestId", "META##RECAG012")).thenReturn(Mono.just(pnEventMeta));
        when(mockPnag012MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che non salvi l'evento
        verify(mockDao, never()).createOrUpdate(pnEventMeta);
        //mi aspetto che faccia lo stesso il flusso PNAG012
        verify(mockPnag012MessageHandler, times(1)).handleMessage(entity, paperRequest);

    }


}
