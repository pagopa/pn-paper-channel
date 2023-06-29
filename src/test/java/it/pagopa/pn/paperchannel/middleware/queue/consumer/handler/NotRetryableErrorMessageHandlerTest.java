package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class NotRetryableErrorMessageHandlerTest {

    private NotRetryableErrorMessageHandler handler;

    private PaperRequestErrorDAO paperRequestErrorDAOMock;
    private SqsSender mockSqsSender;

    @BeforeEach
    public void init() {
        mockSqsSender = mock(SqsSender.class);
        paperRequestErrorDAOMock = mock(PaperRequestErrorDAO.class);
        handler = new NotRetryableErrorMessageHandler(mockSqsSender, paperRequestErrorDAOMock);
    }

    @Test
    void handleMessageTest() {
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(StatusCodeEnum.PROGRESS.getValue())
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);

        when(paperRequestErrorDAOMock.created("requestId", "statusCode", "statusDetails"))
                .thenReturn(Mono.just(new PnRequestError()));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(paperRequestErrorDAOMock, timeout(1000).times(1))
                .created("requestId", StatusCodeEnum.PROGRESS.getValue(), StatusCodeEnum.PROGRESS.getValue());
    }
}
