package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnDematNotValidException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class SaveDematMessageHandlerTest {

    private SaveDematMessageHandler handler;

    private EventDematDAO mockDao;
    private SqsSender mockSqsSender;
    private RequestDeliveryDAO requestDeliveryDAO;
    private PnPaperChannelConfig mockConfig;


    @BeforeEach
    public void init() {
        mockDao = mock(EventDematDAO.class);
        mockSqsSender = mock(SqsSender.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);

        mockConfig = new PnPaperChannelConfig();
        mockConfig.setTtlExecutionDaysDemat(14L);
        mockConfig.setZipHandleActive(false);

        handler = SaveDematMessageHandler.builder()
                .sqsSender(mockSqsSender)
                .eventDematDAO(mockDao)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(mockConfig)
                .build();
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
                        .uri("https://safestorage.it"))
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

        // not call because it is a PROGRESS event
        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CAD", "ARCAD"})
    void handleMessageWithDocumentTypeCADARCADTest(String documentType) {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType(documentType)
                        .date(instant)
                        .uri("https://safestorage.it"))
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

        // not call because it is a PROGRESS event
        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
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
                        .uri("https://safestorage.it"),
                        new AttachmentDetailsDto()
                                .documentType("23L")
                                .date(instant)
                                .uri("https://safestorage.it"))
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
        //mi aspetto che mandi il messaggio a delivery-push per l'evento CAD
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventCAD);
        //mi aspetto che mandi il messaggio a delivery-push per l'evento 23L
        verify(mockSqsSender, times(1)).pushSendEvent(sendEvent23L);
        // not call because it is a PROGRESS event
        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

    @Test
    void handleMessageWithoutAttachmentsTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        StepVerifier.create(handler.handleMessage(entity, paperRequest))
                .expectError(PnDematNotValidException.class)
                .verify();

    }

    @Test
    void handleMessageWithDocumentTypePlicoWithZipHandleActiveTrueTest() {

        mockConfig.setZipHandleActive(true);

        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECRS002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("safestorage://fileKey.zip"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che non salvi l'evento poiché entra in funzione la gestione del demat zip
        verify(mockDao, never()).createOrUpdate(any());
        //mi aspetto che non mandi il messaggio a delivery-push poiché entra in funzione la gestione del demat zip
        verify(mockSqsSender, never()).pushSendEvent(any());

        //mi aspetto che mandi l'evento dematInternalEvent sulla coda interna
        verify(mockSqsSender, times(1)).pushDematZipInternalEvent(any());

        // not call because it is a PROGRESS event
        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

    @Test
    void handleMessageWithDocumentTypePlicoWithZipHandleActiveTrueButMessageFromCallCenterEvolutoTest() {

        mockConfig.setZipHandleActive(true);

        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId(Const.PREFIX_REQUEST_ID_SERVICE_DESK + "requestId")
                .statusCode("RECRS002B")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("safestorage://fileKey.zip"))
                );

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(Const.PREFIX_REQUEST_ID_SERVICE_DESK + "requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PnEventDemat pnEventDematPlico = handler.buildPnEventDemat(paperRequest, paperRequest.getAttachments().get(0));

        SendEvent sendEventPlico = SendEventMapper.createSendEventMessage(entity, getPaperRequestForOneAttachment(paperRequest, paperRequest.getAttachments().get(0)));

        when(mockDao.createOrUpdate(pnEventDematPlico)).thenReturn(Mono.just(pnEventDematPlico));
        MDC.put(Const.CONTEXT_KEY_CLIENT_ID, Const.PREFIX_REQUEST_ID_SERVICE_DESK);

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che salvi l'evento Plico
        verify(mockDao, times(1)).createOrUpdate(pnEventDematPlico);
        //mi aspetto che mandi il messaggio in event bridge per l'evento Plico
        verify(mockSqsSender, times(1)).pushSendEventOnEventBridge(Const.PREFIX_REQUEST_ID_SERVICE_DESK, sendEventPlico);

        // not call because it is a PROGRESS event
        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));

        MDC.clear();
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
