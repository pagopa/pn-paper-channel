package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_API_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryableErrorMessageHandlerTest {

    private RetryableErrorMessageHandler handler;

    private PnPaperChannelConfig mockConfig;
    private ExternalChannelClient mockExtChannel;
    private SqsSender mockSqsSender;
    private PaperRequestErrorDAO mockRequestError;
    private AddressDAO mockAddressDAO;
    private RequestDeliveryDAO requestDeliveryDAO;
    private PcRetryUtils pcRetryUtils;


    @BeforeEach
    public void init() {
        mockSqsSender = mock(SqsSender.class);
        mockExtChannel = mock(ExternalChannelClient.class);
        mockAddressDAO = mock(AddressDAO.class);
        mockRequestError = mock(PaperRequestErrorDAO.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);
        mockConfig = mock(PnPaperChannelConfig.class);
        pcRetryUtils = mock(PcRetryUtils.class);


        handler = RetryableErrorMessageHandler.builder()
                .sqsSender(mockSqsSender)
                .pcRetryUtils(pcRetryUtils)
                .externalChannelClient(mockExtChannel)
                .addressDAO(mockAddressDAO)
                .paperRequestErrorDAO(mockRequestError)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(mockConfig)
                .build();
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
        pnDeliveryRequest.setApplyRasterization(Boolean.TRUE);

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(currentRequestId);
        paperRequest.setStatusDateTime(instant);
        paperRequest.setClientRequestTimeStamp(instant);

        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");

        when(pcRetryUtils.hasOtherAttempt(any())).thenReturn(Boolean.TRUE);
        when(pcRetryUtils.setRetryRequestId(any())).thenReturn("REQUEST.PCRETRY_1");
        when(mockAddressDAO.findAllByRequestId(currentRequestId)).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(), any(), eq(Boolean.TRUE))).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> handler.handleMessage(pnDeliveryRequest, paperRequest).block());

        //verifico che viene invocato ext-channels
        verify(mockExtChannel, timeout(2000).times(1))
                .sendEngageRequest(argThat( (SendRequest sr) -> sr.getRequestId().equals(nextRequestId)), anyList(), anyBoolean() );

        //verifico che viene inviato l'evento a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(argThat((SendEvent se) -> se.getRequestId().equals(currentRequestId) ));

        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

    @Test
    void handleMessageHasNotOtherAttemptTest() {

        // Given
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("request");
        pnDeliveryRequest.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        pnDeliveryRequest.setAttachments(new ArrayList<>());
        pnDeliveryRequest.setProductType(ProductTypeEnum.AR.getValue());
        pnDeliveryRequest.setRequestPaId("0123456789");

        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T16:33:00.000Z");

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId("request.PCRETRY_-2");
        paperRequest.setStatusDateTime(instant);
        paperRequest.setClientRequestTimeStamp(instant);

        // When
        when(pcRetryUtils.hasOtherAttempt(any())).thenReturn(Boolean.FALSE);
        when(mockRequestError.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));

        // Then
        assertDoesNotThrow(() -> handler.handleMessage(pnDeliveryRequest, paperRequest).block());

        //verifico che viene NON invocato ext-channels
        verify(mockExtChannel, timeout(2000).times(0)).sendEngageRequest(any(SendRequest.class), anyList(), eq(null));

        //verifico che viene salvata la richiesta andata in errore
        verify(mockRequestError, timeout(2000).times(1))
                .created(argThat(requestError ->
                    requestError.getRequestId().equals(pnDeliveryRequest.getRequestId()) &&
                    requestError.getPaId().equals(pnDeliveryRequest.getRequestPaId()) &&
                    requestError.getError().equals(EXTERNAL_CHANNEL_API_EXCEPTION.getMessage()) &&
                    requestError.getFlowThrow().equals(EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                ));

        //verifico che viene inviato l'evento a delivery-push
        verify(mockSqsSender, times(1)).pushSendEvent(any(SendEvent.class));

        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

}
