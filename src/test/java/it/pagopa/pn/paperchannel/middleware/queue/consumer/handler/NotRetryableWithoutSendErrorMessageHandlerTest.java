package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class NotRetryableWithoutSendErrorMessageHandlerTest {

    private NotRetriableWithoutSendErrorMessageHandler handler;

    @MockBean
    private PaperRequestErrorDAO paperRequestErrorDAOMock;

    @BeforeEach
    public void init() {
        paperRequestErrorDAOMock = mock(PaperRequestErrorDAO.class);
        handler = NotRetriableWithoutSendErrorMessageHandler.builder()
                .paperRequestErrorDAO(paperRequestErrorDAOMock)
                .build();
    }

    @Test
    void handleMessageTest() {

        // Given
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        entity.setRequestPaId("0123456789");

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        PnRequestError pnRequestError = new PnRequestError();
        pnRequestError.setRequestId("requestId");

        // When
        when(paperRequestErrorDAOMock.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(pnRequestError));

        // Then
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(paperRequestErrorDAOMock, timeout(1000).times(1))
                .created(argThat(requestError ->
                    requestError.getRequestId().equals(entity.getRequestId()) &&
                    requestError.getPaId().equals(entity.getRequestPaId()) &&
                    requestError.getError().equals(entity.getStatusCode()) &&
                    requestError.getFlowThrow().equals(entity.getStatusDetail())
                ));
    }
}

