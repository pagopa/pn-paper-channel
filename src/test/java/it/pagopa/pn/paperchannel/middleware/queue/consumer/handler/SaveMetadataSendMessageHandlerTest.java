package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SaveMetadataSendMessageHandlerTest {
    private static final PnPaperChannelConfig mockConfig = mock(PnPaperChannelConfig.class);
    private static final EventMetaDAO mockEventMetaDAO = mock(EventMetaDAO.class);
    private static final SqsSender mockSqsSender = mock(SqsSender.class);
    private static final RequestDeliveryDAO mockRequestDeliveryDAO = mock(RequestDeliveryDAO.class);
    private static final PnEventErrorDAO mockPnEventErrorDAO = mock(PnEventErrorDAO.class);
    private static ChainedMessageHandler handler;
    private static SaveMetadataMessageHandler saveMetadataMessageHandler;

    @BeforeAll
    public static void init(){
        long ttlDays = 365;
        when(mockConfig.getTtlExecutionDaysMeta()).thenReturn(ttlDays);

        saveMetadataMessageHandler = SaveMetadataMessageHandler.builder()
                .eventMetaDAO(mockEventMetaDAO)
                .pnPaperChannelConfig(mockConfig)
                .build();
        SendToDeliveryPushHandler sendToDeliveryPushHandler = SendToDeliveryPushHandler.builder()
                .sqsSender(mockSqsSender)
                .requestDeliveryDAO(mockRequestDeliveryDAO)
                .pnPaperChannelConfig(mockConfig)
                .pnEventErrorDAO(mockPnEventErrorDAO)
                .build();
        handler = ChainedMessageHandler.builder()
                .handlers(List.of(saveMetadataMessageHandler, sendToDeliveryPushHandler))
                .build();
    }

    @Test
    void handleMessage_progress_ok() {
        // Arrange
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("some");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        PnEventMeta pnEventMeta = saveMetadataMessageHandler.buildPnEventMeta(paperRequest);
        when(mockEventMetaDAO.createOrUpdate(pnEventMeta)).thenReturn(Mono.just(pnEventMeta));

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        // Assert
        // I expect it to save the event
        verify(mockEventMetaDAO, times(1)).createOrUpdate(pnEventMeta);

        // I expect it to send the message to delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId(Mockito.anyString());

        // Not called because it is a PROGRESS event
        verify(mockRequestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(Mockito.anyString(), Mockito.any(Instant.class));
    }
}
