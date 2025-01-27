package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrepareFlowStarterImplTest {

    @Mock
    private SqsSender sqsSender;

    @Mock
    private PnPaperChannelConfig config;

    @InjectMocks
    private PrepareFlowStarterImpl prepareFlowStarterImpl;

    @Test
    void startPreparePhaseOneFromPrepareSyncWithPrepareTwoPhasesTrueTest() {
        when(config.isPrepareTwoPhases()).thenReturn(true);

        var clientId = "clientId";
        var deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setIun("iun");

        final PrepareNormalizeAddressEvent expectedEvent = PrepareNormalizeAddressEvent.builder()
                .requestId("requestId")
                .iun("iun")
                .clientId("clientId")
                .isAddressRetry(false)
                .attempt(0)
                .correlationId(null)
                .build();

        assertThatCode(() -> prepareFlowStarterImpl.startPreparePhaseOneFromPrepareSync(deliveryRequest, clientId)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToNormalizeAddressQueue(expectedEvent);
        verify(sqsSender, never()).pushToInternalQueue(any(PrepareAsyncRequest.class));


    }

    @Test
    void startPreparePhaseOneFromPrepareSyncWithPrepareTwoPhasesFalseTest() {
        when(config.isPrepareTwoPhases()).thenReturn(false);

        var clientId = "clientId";
        var deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setIun("iun");

        var expectedEvent = new PrepareAsyncRequest("requestId", "iun", false, 0);

        assertThatCode(() -> prepareFlowStarterImpl.startPreparePhaseOneFromPrepareSync(deliveryRequest, clientId)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToInternalQueue(expectedEvent);
        verify(sqsSender, never()).pushToNormalizeAddressQueue(any(PrepareNormalizeAddressEvent.class));

    }

    @Test
    void startPreparePhaseOneFromNationalRegistriesFlowWithPrepareTwoPhasesTrueWithAddressNotNullTest() {
        when(config.isPrepareTwoPhases()).thenReturn(true);

        var deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setCorrelationId("correlationId");
        Address address = new Address();
        address.setAddress("via Roma 12");

        final PrepareNormalizeAddressEvent expectedEvent = PrepareNormalizeAddressEvent.builder()
                .requestId("requestId")
                .correlationId("correlationId")
                .address(address)
                .build();

        assertThatCode(() -> prepareFlowStarterImpl.startPreparePhaseOneFromNationalRegistriesFlow(deliveryRequest, address)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToNormalizeAddressQueue(expectedEvent);
        verify(sqsSender, never()).pushToInternalQueue(any(PrepareAsyncRequest.class));

    }

    @Test
    void startPreparePhaseOneFromNationalRegistriesFlowWithPrepareTwoPhasesTrueWithAddressNullTest() {
        when(config.isPrepareTwoPhases()).thenReturn(true);

        var deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setCorrelationId("correlationId");

        final PrepareNormalizeAddressEvent expectedEvent = PrepareNormalizeAddressEvent.builder()
                .requestId("requestId")
                .correlationId("correlationId")
                .address(null)
                .build();

        assertThatCode(() -> prepareFlowStarterImpl.startPreparePhaseOneFromNationalRegistriesFlow(deliveryRequest, null)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToNormalizeAddressQueue(expectedEvent);
        verify(sqsSender, never()).pushToInternalQueue(any(PrepareAsyncRequest.class));

    }

    @Test
    void startPreparePhaseOneFromNationalRegistriesFlowWithPrepareTwoPhasesFalseWithAddressNotNullTest() {
        when(config.isPrepareTwoPhases()).thenReturn(false);

        var deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setCorrelationId("correlationId");
        Address address = new Address();
        address.setAddress("via Roma 12");

        var expectedEvent = new PrepareAsyncRequest("requestId", "correlationId", address);

        assertThatCode(() -> prepareFlowStarterImpl.startPreparePhaseOneFromNationalRegistriesFlow(deliveryRequest, address)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToInternalQueue(expectedEvent);
        verify(sqsSender, never()).pushToNormalizeAddressQueue(any(PrepareNormalizeAddressEvent.class));

    }

    @Test
    void startPreparePhaseOneFromNationalRegistriesFlowWithPrepareTwoPhasesFalseWithAddressNullTest() {
        when(config.isPrepareTwoPhases()).thenReturn(false);

        var deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setCorrelationId("correlationId");

        var expectedEvent = new PrepareAsyncRequest("requestId", "correlationId", null);

        assertThatCode(() -> prepareFlowStarterImpl.startPreparePhaseOneFromNationalRegistriesFlow(deliveryRequest, null)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToInternalQueue(expectedEvent);

    }

}
