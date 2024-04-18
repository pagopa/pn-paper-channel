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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class DirectlySendMessageHandlerTest {

    private DirectlySendMessageHandler handler;

    private SqsSender mockSqsSender;
    private RequestDeliveryDAO requestDeliveryDAO;

    @BeforeEach
    public void init() {
        mockSqsSender = mock(SqsSender.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);

        handler = DirectlySendMessageHandler.builder()
                .sqsSender(mockSqsSender)
                .requestDeliveryDAO(requestDeliveryDAO)
                .build();
    }

    @ParameterizedTest
    @MethodSource(value = "directlySendMessageHandlerTestCases")
    void handleMessageTest(PaperProgressStatusEventDto paperProgressStatusEventDto) {

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(paperProgressStatusEventDto.getRequestId());
        entity.setStatusCode(paperProgressStatusEventDto.getStatusCode());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperProgressStatusEventDto).block());

        // expect exactly one call to delivery push sqs queue
        SendEvent sendEventExpected = SendEventMapper.createSendEventMessage(entity, paperProgressStatusEventDto);
        verify(mockSqsSender, times(1)).pushSendEvent(sendEventExpected);
        verify(requestDeliveryDAO, never()).updateData(entity);
    }

    /**
     * Build test argument cases for {@link DirectlySendMessageHandlerTest#handleMessageTest}
     * */
    private static Stream<Arguments> directlySendMessageHandlerTestCases() {
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
