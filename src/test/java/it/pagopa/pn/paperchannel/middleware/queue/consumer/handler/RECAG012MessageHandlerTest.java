package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

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

    private EventMetaDAO mockDao;

    private SaveMetadataMessageHandler handler;


    @BeforeEach
    public void init() {
        mockDao = mock(EventMetaDAO.class);
        long ttlDays = 365;
        handler = new RECAG012MessageHandler(mockDao, ttlDays);
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

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento
        verify(mockDao, times(1)).createOrUpdate(pnEventMeta);

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
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento
        verify(mockDao, times(0)).createOrUpdate(pnEventMeta);

    }


}
