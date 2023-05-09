package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class SaveDematMessageHandlerTest {

    private EventDematDAO mockDao;

    private SqsSender mockSqsSender;

    private SaveDematMessageHandler handler;


    @BeforeEach
    public void init() {
        mockDao = mock(EventDematDAO.class);
        mockSqsSender = mock(SqsSender.class);
        long ttlDays = 365;
        handler = new SaveDematMessageHandler(mockSqsSender, mockDao, ttlDays);
    }

    @Test
    void handleMessageWithDocumentTypePlicoTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECRS002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .url("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PnEventDemat pnEventDemat = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        when(mockDao.createOrUpdate(pnEventDemat)).thenReturn(Mono.just(pnEventDemat));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento
        verify(mockDao, times(1)).createOrUpdate(pnEventDemat);
        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);

    }

    @Test
    void handleMessageWithDocumentTypeCADTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("CAD")
                        .date(instant)
                        .url("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PnEventDemat pnEventDemat = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        SendEvent sendEventNotExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        when(mockDao.createOrUpdate(pnEventDemat)).thenReturn(Mono.just(pnEventDemat));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento
        verify(mockDao, times(1)).createOrUpdate(pnEventDemat);
        //mi aspetto che non mandi il messaggio a delivery-push
        verify(mockSqsSender, times(0)).pushSendEvent(sendEventNotExpected);

    }

    @Test
    void handleMessageWithDocumentTypeCADAnd23LTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(
                        new AttachmentDetailsDto()
                        .documentType("CAD")
                        .date(instant)
                        .url("https://safestorage.it"),
                        new AttachmentDetailsDto()
                                .documentType("23L")
                                .date(instant)
                                .url("https://safestorage.it"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());


        PnEventDemat pnEventDematCAD = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));
        PnEventDemat pnEventDemat23L = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(1));

        SendEvent sendEventCAD = SendEventMapper.createSendEventMessage(entity, getPaperRequestForOneAttachment(paperRequest, paperRequest.getAttachments().get(0)));
        SendEvent sendEvent23L = SendEventMapper.createSendEventMessage(entity, getPaperRequestForOneAttachment(paperRequest, paperRequest.getAttachments().get(1)));

        when(mockDao.createOrUpdate(pnEventDematCAD)).thenReturn(Mono.just(pnEventDematCAD));
        when(mockDao.createOrUpdate(pnEventDemat23L)).thenReturn(Mono.just(pnEventDemat23L));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento CAD
        verify(mockDao, times(1)).createOrUpdate(pnEventDematCAD);
        //mi aspetto che salvi l'evento Plico
        verify(mockDao, times(1)).createOrUpdate(pnEventDemat23L);
        //mi aspetto che NON mandi il messaggio a delivery-push per l'evento CAD
        verify(mockSqsSender, times(0)).pushSendEvent(sendEventCAD);
        //mi aspetto che mandi il messaggio a delivery-push per l'evento 23L
        verify(mockSqsSender, times(1)).pushSendEvent(sendEvent23L);

    }

    private PaperProgressStatusEventDto getPaperRequestForOneAttachment(
            PaperProgressStatusEventDto paperRequest, AttachmentDetailsDto attachmentDetailsDto) {

       return new PaperProgressStatusEventDto()
                .requestId(paperRequest.getRequestId())
                .statusCode(paperRequest.getStatusCode())
                .statusDateTime(paperRequest.getStatusDateTime())
                .clientRequestTimeStamp(paperRequest.getClientRequestTimeStamp())
                .attachments(List.of(attachmentDetailsDto));

    }

}
