package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class RECAG011AMessageHandlerTest {

    private EventMetaDAO mockDao;

    private SaveMetadataMessageHandler handler;

    private SqsSender mockSqsSender;


    @BeforeEach
    public void init() {
        mockDao = mock(EventMetaDAO.class);
        mockSqsSender = mock(SqsSender.class);

        long ttlDays = 365;

        handler = new RECAG011AMessageHandler(mockSqsSender, mockDao, ttlDays);
    }

    @Test
    void handleMessageTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG011A")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PnEventMeta pnEventMeta = handler.buildPnEventMeta(paperRequest);

        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        when(mockDao.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // mi aspetto che salvi l'evento
        verify(mockDao, times(1)).createOrUpdate(pnEventMeta);

        // mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
    }
}
