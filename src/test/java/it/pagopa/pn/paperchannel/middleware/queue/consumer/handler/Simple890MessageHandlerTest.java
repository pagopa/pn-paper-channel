package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;


import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class Simple890MessageHandlerTest {

    private Simple890MessageHandler handler;

    private SqsSender mockSqsSender;
    private RequestDeliveryDAO requestDeliveryDAO;


    @BeforeEach
    public void init() {
        mockSqsSender = mock(SqsSender.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);

        handler = Simple890MessageHandler.builder()
                .requestDeliveryDAO(requestDeliveryDAO)
                .sqsSender(mockSqsSender)
                .build();
    }

    @Test
    void handleMessageTest() {
        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG005C")
                .statusDateTime(OffsetDateTime.parse("2023-08-04T14:44:00.000Z"))
                .clientRequestTimeStamp(OffsetDateTime.parse("2023-08-04T14:44:00.000Z"))
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(OffsetDateTime.parse("2023-08-04T14:44:00.000Z"))
                        .uri("https://safestorage.it")
                ));

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(paperProgressStatusEventDto.getRequestId());
        entity.setStatusCode(paperProgressStatusEventDto.getStatusCode());
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperProgressStatusEventDto).block());

        // expect exactly one call to delivery push sqs queue
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperProgressStatusEventDto);
        sendEventExpected.setStatusCode(StatusCodeEnum.PROGRESS);

        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(requestDeliveryDAO, never()).updateData(entity);
    }


}
