package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.InvalidEventOrderException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

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

    @Test
    void handleMessage_ko_throws_InvalidEventOrderException(){
        // Arrange
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setFeedbackStatusCode("RECRN001C");

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(entity.getRequestId());
        paperRequest.setStatusCode("RECRN001A");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));

        // Act / Assert
        InvalidEventOrderException exception = assertThrows(InvalidEventOrderException.class,
                ()-> handler.handleMessage(entity, paperRequest).block());

        // Assert feedback status
        assertEquals("RECRN001C", exception.getFeedbackStatus().oldFeedbackStatusCode());
        assertEquals("RECRN001A", exception.getFeedbackStatus().newFeedbackStatusCode());
    }

    @ParameterizedTest
    @MethodSource(value = "handleProgressMessageTestCases")
    void handleProgressMessage(PaperProgressStatusEventDto paperProgressStatusEventDto) {
        // Arrange
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(paperProgressStatusEventDto.getRequestId());
        entity.setStatusCode(paperProgressStatusEventDto.getStatusCode());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        // Act / Assert
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperProgressStatusEventDto).block());

        // expect exactly one call to delivery push sqs queue
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperProgressStatusEventDto);
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(requestDeliveryDAO, never()).updateData(entity);
    }

    /**
     * Build test argument cases for {@link SendToDeliveryPushHandlerTest#handleProgressMessage}
     * */
    private static Stream<Arguments> handleProgressMessageTestCases() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");

        /* Test inputs */
        PaperProgressStatusEventDto con080ProgressStatusEventDto = buildPaperProgressStatusEventDto("CON080", instant,
                List.of(new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(instant)
                        .uri("https://safestorage.it")
                )
        );

        PaperProgressStatusEventDto recri001ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRI001", instant, null);
        PaperProgressStatusEventDto recri002ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRI002", instant, null);
        PaperProgressStatusEventDto recrs001cProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRS001C", instant, null);
        PaperProgressStatusEventDto recrs003cProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRS003C", instant, null);
        PaperProgressStatusEventDto recrs015ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRS015", instant, null);
        PaperProgressStatusEventDto recrn015ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRN015", instant, null);
        PaperProgressStatusEventDto recag015ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECAG015", instant, null);
        PaperProgressStatusEventDto recag010ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECAG010", instant, null);
        PaperProgressStatusEventDto recrs010ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRS010", instant, null);
        PaperProgressStatusEventDto recrn010ProgressStatusEventDto = buildPaperProgressStatusEventDto("RECRN010", instant, null);

        /* Test method arguments */
        Arguments con080ProgressStatusEventArguments = Arguments.of(con080ProgressStatusEventDto);
        Arguments recri001ProgressStatusEventArguments = Arguments.of(recri001ProgressStatusEventDto);
        Arguments recri002ProgressStatusEventArguments = Arguments.of(recri002ProgressStatusEventDto);
        Arguments recrs001cProgressStatusEventArguments = Arguments.of(recrs001cProgressStatusEventDto);
        Arguments recrs003cProgressStatusEventArguments = Arguments.of(recrs003cProgressStatusEventDto);
        Arguments recrs015ProgressStatusEventArguments = Arguments.of(recrs015ProgressStatusEventDto);
        Arguments recrn015ProgressStatusEventArguments = Arguments.of(recrn015ProgressStatusEventDto);
        Arguments recag015ProgressStatusEventArguments = Arguments.of(recag015ProgressStatusEventDto);
        Arguments recag010ProgressStatusEventArguments = Arguments.of(recag010ProgressStatusEventDto);
        Arguments recrs010ProgressStatusEventArguments = Arguments.of(recrs010ProgressStatusEventDto);
        Arguments recrn010ProgressStatusEventArguments = Arguments.of(recrn010ProgressStatusEventDto);

        return Stream.of(
                con080ProgressStatusEventArguments,
                recri001ProgressStatusEventArguments,
                recri002ProgressStatusEventArguments,
                recrs001cProgressStatusEventArguments,
                recrs003cProgressStatusEventArguments,
                recrs015ProgressStatusEventArguments,
                recrn015ProgressStatusEventArguments,
                recag015ProgressStatusEventArguments,
                recag010ProgressStatusEventArguments,
                recrs010ProgressStatusEventArguments,
                recrn010ProgressStatusEventArguments
        );
    }

    private static PaperProgressStatusEventDto buildPaperProgressStatusEventDto(String statusCode, OffsetDateTime instant, List<AttachmentDetailsDto> attachmentDetailsDtoList) {
        return new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(statusCode)
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .attachments(attachmentDetailsDtoList);
    }
}