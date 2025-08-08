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
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.SendProgressMetaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CON018ChainedMessageHandlerTest {
    private SqsSender mockSqsSender;
    private RequestDeliveryDAO mockRequestDeliveryDAO;
    private PnEventErrorDAO mockPnEventErrorDAO;
    private SendProgressMetaConfig mockSendProgressMetaConfig;
    private HandlersFactory handlersFactory;

    @BeforeEach
    public void init(){
        PnPaperChannelConfig mockConfig = mock(PnPaperChannelConfig.class);
        EventMetaDAO mockEventMetaDAO = mock(EventMetaDAO.class);
        mockSqsSender = mock(SqsSender.class);
        mockRequestDeliveryDAO = mock(RequestDeliveryDAO.class);
        mockPnEventErrorDAO = mock(PnEventErrorDAO.class);
        mockSendProgressMetaConfig = mock(SendProgressMetaConfig.class);

        long ttlDays = 365;
        when(mockConfig.getTtlExecutionDaysMeta()).thenReturn(ttlDays);

        handlersFactory = new HandlersFactory(null, null, null,null,
                mockConfig, mockSqsSender, mockEventMetaDAO, null, null,
                mockRequestDeliveryDAO, mockPnEventErrorDAO, mockSendProgressMetaConfig, null,
                null, null);
    }

    @Test
    void handleMessage_ok_CON018Enabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(true);
        when(mockSendProgressMetaConfig.isCON018Enabled()).thenReturn(true);
        handlersFactory.initializeHandlers();

        MessageHandler handler = handlersFactory.getHandler(ExternalChannelCodeEnum.CON018.name());

        // Arrange
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(ExternalChannelCodeEnum.CON018.name());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode(ExternalChannelCodeEnum.CON018.name());
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // Assert
        // I expect it to send the message to delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId(Mockito.anyString());

        // Not called because it is a PROGRESS event
        verify(mockRequestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(Mockito.anyString(), Mockito.any(Instant.class));
    }

    @Test
    void handleMessage_ok_CON018Disabled() {
        when(mockSendProgressMetaConfig.isMetaEnabled()).thenReturn(false);
        when(mockSendProgressMetaConfig.isCON018Enabled()).thenReturn(false);
        handlersFactory.initializeHandlers();

        MessageHandler handler = handlersFactory.getHandler(ExternalChannelCodeEnum.CON018.name());

        // Arrange
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(ExternalChannelCodeEnum.CON018.name());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode(ExternalChannelCodeEnum.CON018.name());
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        // Assert
        // I expect it to not send the message to delivery-push
        verify(mockSqsSender, never()).pushSendEvent(any());
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId(Mockito.anyString());

        // Not called because it is a PROGRESS event
        verify(mockRequestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(mockPnEventErrorDAO, never()).deleteItem(Mockito.anyString(), Mockito.any(Instant.class));
    }
}
