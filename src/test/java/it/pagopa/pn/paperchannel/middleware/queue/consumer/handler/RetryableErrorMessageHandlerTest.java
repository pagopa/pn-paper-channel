package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_API_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class RetryableErrorMessageHandlerTest {

    private RetryableErrorMessageHandler handler;

    private PnPaperChannelConfig mockConfig;

    private ExternalChannelClient mockExtChannel;

    private SqsSender mockSqsSender;

    private PaperRequestErrorDAO mockRequestError;

    private AddressDAO mockAddressDAO;

    private PnClientDAO pnClientDAO;


    @BeforeEach
    public void init() {
        mockSqsSender = mock(SqsSender.class);
        mockExtChannel = mock(ExternalChannelClient.class);
        mockAddressDAO = mock(AddressDAO.class);
        mockRequestError = mock(PaperRequestErrorDAO.class);
        mockConfig = mock(PnPaperChannelConfig.class);
        pnClientDAO = mock(PnClientDAO.class);

        handler = new RetryableErrorMessageHandler(mockSqsSender, mockExtChannel, mockAddressDAO, mockRequestError, mockConfig, pnClientDAO);
    }

    @Test
    void handleMessageHasOtherAttemptTest() {

        String currentRequestId = "REQUEST.PCRETRY_0";
        String nextRequestId = "REQUEST.PCRETRY_1";
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T16:33:00.000Z");
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId(currentRequestId);
        pnDeliveryRequest.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        pnDeliveryRequest.setAttachments(new ArrayList<>());
        pnDeliveryRequest.setProductType(ProductTypeEnum.AR.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(currentRequestId);
        paperRequest.setStatusDateTime(instant);
        paperRequest.setClientRequestTimeStamp(instant);

        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");

        when(mockConfig.getAttemptQueueExternalChannel()).thenReturn(1);
        when(mockAddressDAO.findAllByRequestId(currentRequestId)).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(SendRequest.class), anyList())).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> handler.handleMessage(pnDeliveryRequest, paperRequest).block());

        //verifico che viene invocato ext-channels
        verify(mockExtChannel, timeout(2000).times(1))
                .sendEngageRequest(argThat( (SendRequest sr) -> sr.getRequestId().equals(nextRequestId)), anyList() );

        //verifico che viene inviato l'evento a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(argThat((SendEvent se) -> se.getRequestId().equals(currentRequestId) ));
    }

    @Test
    void handleMessageHasNotOtherAttemptTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T16:33:00.000Z");
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("request");
        pnDeliveryRequest.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        pnDeliveryRequest.setAttachments(new ArrayList<>());
        pnDeliveryRequest.setProductType(ProductTypeEnum.AR.getValue());

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId("request.PCRETRY_-2");
        paperRequest.setStatusDateTime(instant);
        paperRequest.setClientRequestTimeStamp(instant);

        when(mockConfig.getAttemptQueueExternalChannel()).thenReturn(-1);
        when(mockRequestError.created("request", EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(),
                EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name() )).thenReturn(Mono.just(new PnRequestError()));

        assertDoesNotThrow(() -> handler.handleMessage(pnDeliveryRequest, paperRequest).block());

        //verifico che viene NON invocato ext-channels
        verify(mockExtChannel, timeout(2000).times(0)).sendEngageRequest(any(SendRequest.class), anyList());

        //verifico che viene salvata la richiesta andata in errore
        verify(mockRequestError, timeout(2000).times(1)).created(pnDeliveryRequest.getRequestId(),
                EXTERNAL_CHANNEL_API_EXCEPTION.getMessage(), EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name());

        //verifico che viene inviato l'evento a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(any(SendEvent.class));
    }

}
