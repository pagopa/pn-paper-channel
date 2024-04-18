package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

class SendToDeliveryPushHandlerTest {


    private SendToDeliveryPushHandler handler;

    private PnEventErrorDAO eventErrorDAO;
    private SqsSender mockSqsSender;
    private RequestDeliveryDAO requestDeliveryDAO;

    private PnPaperChannelConfig pnPaperChannelConfig;

    @BeforeEach
    public void init(){

        eventErrorDAO = Mockito.mock(PnEventErrorDAO.class);

        mockSqsSender = mock(SqsSender.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);


        pnPaperChannelConfig = mock(PnPaperChannelConfig.class);

        handler = TestSendToDeliveryPushHandler.builder()
                .pnEventErrorDAO(eventErrorDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .sqsSender(mockSqsSender)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();

    }


    @Test
    void handleMessage_progress() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("some");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);


        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(eventErrorDAO, never()).findEventErrorsByRequestId(Mockito.anyString());

        // not call because it is a PROGRESS event
        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(eventErrorDAO, never()).deleteItem(Mockito.anyString(), Mockito.any(Instant.class));
    }

    @Test
    void handleMessage_ok_noerrorcodesconf() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("someok");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any(), anyBoolean())).thenReturn(Mono.just(entity));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(eventErrorDAO, never()).findEventErrorsByRequestId(Mockito.anyString());

        ArgumentCaptor<PnDeliveryRequest> argumentCaptor = ArgumentCaptor.forClass(PnDeliveryRequest.class);
        verify(requestDeliveryDAO, times(1)).updateData(argumentCaptor.capture(), eq(true));

        verify(eventErrorDAO, never()).deleteItem(Mockito.anyString(), Mockito.any(Instant.class));

        Assertions.assertTrue(argumentCaptor.getValue().getRefined());
    }

    @Test
    void handleMessage_ok_witherrorcodes_noerrortosend() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("someok");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any(), anyBoolean())).thenReturn(Mono.just(entity));
        Mockito.when(pnPaperChannelConfig.getAllowedRedriveProgressStatusCodes()).thenReturn(List.of("SOME1"));
        Mockito.when(eventErrorDAO.findEventErrorsByRequestId(Mockito.anyString())).thenReturn(Flux.empty());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, never()).pushSingleStatusUpdateEvent(Mockito.any());
        verify(eventErrorDAO, never()).deleteItem(Mockito.anyString(), Mockito.any(Instant.class));

        verify(requestDeliveryDAO, times(1)).updateData(any(PnDeliveryRequest.class), eq(true));
    }


    @Test
    void handleMessage_ok_witherrorcodes_1tosend() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("someok");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        PnEventError error = new PnEventError();
        PaperProgressStatusEventOriginalMessageInfo paperProgressStatusEventOriginalMessageInfo = new PaperProgressStatusEventOriginalMessageInfo();
        paperProgressStatusEventOriginalMessageInfo.setEventType("EVENT");
        paperProgressStatusEventOriginalMessageInfo.setStatusCode("SOME1");
        paperProgressStatusEventOriginalMessageInfo.setStatusDateTime(Instant.now());
        paperProgressStatusEventOriginalMessageInfo.setClientRequestTimeStamp(Instant.now());
        paperProgressStatusEventOriginalMessageInfo.setStatusDescription("Some description");

        error.setRequestId(paperRequest.getRequestId());
        error.setStatusBusinessDateTime(Instant.now());
        error.setOriginalMessageInfo(paperProgressStatusEventOriginalMessageInfo);
        error.setStatusCode(paperProgressStatusEventOriginalMessageInfo.getStatusCode());

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any(), anyBoolean())).thenReturn(Mono.just(entity));
        Mockito.when(pnPaperChannelConfig.getAllowedRedriveProgressStatusCodes()).thenReturn(List.of("SOME1"));
        Mockito.when(eventErrorDAO.findEventErrorsByRequestId(Mockito.any())).thenReturn(Flux.fromIterable(List.of(error)));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, times(1)).pushSingleStatusUpdateEvent(Mockito.any());
        verify(eventErrorDAO, times(1)).deleteItem(Mockito.anyString(), Mockito.any(Instant.class));

        verify(requestDeliveryDAO, times(1)).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(eventErrorDAO, times(1)).deleteItem(anyString(), any(Instant.class));
    }


    @Test
    void handleMessage_ok_witherrorcodes_1nottosend() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("someok");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        PnEventError error = new PnEventError();
        PaperProgressStatusEventOriginalMessageInfo paperProgressStatusEventOriginalMessageInfo = new PaperProgressStatusEventOriginalMessageInfo();
        paperProgressStatusEventOriginalMessageInfo.setEventType("EVENT");
        paperProgressStatusEventOriginalMessageInfo.setStatusCode("SOME1");
        paperProgressStatusEventOriginalMessageInfo.setStatusDateTime(Instant.now());
        paperProgressStatusEventOriginalMessageInfo.setClientRequestTimeStamp(Instant.now());
        paperProgressStatusEventOriginalMessageInfo.setStatusDescription("Some description");

        error.setRequestId(paperRequest.getRequestId());
        error.setStatusBusinessDateTime(Instant.now());
        error.setOriginalMessageInfo(paperProgressStatusEventOriginalMessageInfo);
        error.setStatusCode(paperProgressStatusEventOriginalMessageInfo.getStatusCode());

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any(), anyBoolean())).thenReturn(Mono.just(entity));
        Mockito.when(pnPaperChannelConfig.getAllowedRedriveProgressStatusCodes()).thenReturn(List.of("SOME2"));
        Mockito.when(eventErrorDAO.findEventErrorsByRequestId(Mockito.any())).thenReturn(Flux.fromIterable(List.of(error)));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, times(0)).pushSingleStatusUpdateEvent(Mockito.any());

        verify(requestDeliveryDAO, times(1)).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(eventErrorDAO, never()).deleteItem(anyString(), any(Instant.class));
    }

    @Test
    void handleMessage_ok_witherrorcodes_1nottosend_badclass() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("someok");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        PnEventError error = new PnEventError();
        OriginalMessageInfo paperProgressStatusEventOriginalMessageInfo = new OriginalMessageInfo();
        paperProgressStatusEventOriginalMessageInfo.setEventType("EVENT");

        error.setRequestId(paperRequest.getRequestId());
        error.setStatusBusinessDateTime(Instant.now());
        error.setStatusCode("SOME1");

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any(), anyBoolean())).thenReturn(Mono.just(entity));
        Mockito.when(pnPaperChannelConfig.getAllowedRedriveProgressStatusCodes()).thenReturn(List.of("SOME1"));
        Mockito.when(eventErrorDAO.findEventErrorsByRequestId(Mockito.any())).thenReturn(Flux.fromIterable(List.of(error)));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperRequest);

        //mi aspetto che mandi il messaggio a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(mockSqsSender, times(0)).pushSingleStatusUpdateEvent(Mockito.any());

        verify(requestDeliveryDAO, times(1)).updateData(any(PnDeliveryRequest.class), eq(true));
        verify(eventErrorDAO, never()).deleteItem(anyString(), any(Instant.class));
    }
}