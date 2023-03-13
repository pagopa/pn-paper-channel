package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
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
        handler = new SaveMetadataMessageHandler(mockDao, ttlDays);
    }

    @Test
    void handleMessageTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG012")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusDetail("statusDetail");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());

        PnEventMeta pnEventMeta = handler.buildPnEventMeta(paperRequest);

        when(mockDao.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento
        verify(mockDao, times(1)).createOrUpdate(pnEventMeta);

    }


}
