package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class RECAG012AMessageHandlerTest {

    private RECAG012AMessageHandler handler;

    private PnEventErrorDAO mockPnEventErrorDAO;
    private SqsSender mockSqsSender;

    @BeforeEach
    public void init(){
        mockPnEventErrorDAO = mock(PnEventErrorDAO.class);
        mockSqsSender = mock(SqsSender.class);
        RequestDeliveryDAO mockRequestDeliveryDAO = mock(RequestDeliveryDAO.class);
        PnPaperChannelConfig pnPaperChannelConfig = mock(PnPaperChannelConfig.class);

        handler = RECAG012AMessageHandler.builder()
                .pnEventErrorDAO(mockPnEventErrorDAO)
                .requestDeliveryDAO(mockRequestDeliveryDAO)
                .sqsSender(mockSqsSender)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
    }

    @Test
    void handleMessage_refined_false() {
        // Arrange
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(ExternalChannelCodeEnum.RECAG012.name())
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        // Assert
        // I expect entity and paperRequest to be changed
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), entity.getStatusCode());
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), entity.getStatusDescription());
        assertEquals(StatusCodeEnum.PROGRESS.getValue(), entity.getStatusDetail());
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), paperRequest.getStatusCode());
        assertEquals(ExternalChannelCodeEnum.RECAG012A.name(), paperRequest.getStatusDescription());

        // I expect it to send the message to delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId(Mockito.anyString());
    }

    @Test
    void handleMessage_refined_true() {
        // Arrange
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(ExternalChannelCodeEnum.RECAG012.name())
                .statusDescription(ExternalChannelCodeEnum.RECAG012.name())
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(ExternalChannelCodeEnum.RECAG012.name());
        entity.setStatusDescription(ExternalChannelCodeEnum.RECAG012.name());
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(true);

        // Act
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        // Assert
        // I expect entity and paperRequest to not be changed
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), entity.getStatusCode());
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), entity.getStatusDescription());
        assertEquals(StatusCodeEnum.OK.getValue(), entity.getStatusDetail());
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), paperRequest.getStatusCode());
        assertEquals(ExternalChannelCodeEnum.RECAG012.name(), paperRequest.getStatusDescription());

        // I expect it to not send the message to delivery-push
        verify(mockSqsSender, never()).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(mockPnEventErrorDAO, never()).findEventErrorsByRequestId(Mockito.anyString());
    }
}
