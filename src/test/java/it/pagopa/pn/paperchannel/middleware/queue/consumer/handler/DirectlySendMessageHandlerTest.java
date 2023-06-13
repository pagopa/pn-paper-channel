package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class DirectlySendMessageHandlerTest {

    private DirectlySendMessageHandler handler;

    private SqsSender mockSqsSender;

    @BeforeEach
    public void init() {
        mockSqsSender = mock(SqsSender.class);
        handler = new DirectlySendMessageHandler(mockSqsSender);
    }

    @Test
    void handleMessageTest_CON080() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("CON080")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che mandi il messaggio a delivery-push
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
    }


    @Test
    void handleMessageTest_RECRI001() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECRI001")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                ;

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che mandi il messaggio a delivery-push
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
    }

    @Test
    void handleMessageTest_RECRI002() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECRI002")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                ;

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che mandi il messaggio a delivery-push
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
    }
}
