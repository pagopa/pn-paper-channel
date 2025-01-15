package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
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
class PrepareUtilTest {

    @Mock
    private SqsSender sqsSender;

    @Mock
    private PnPaperChannelConfig config;

    @InjectMocks
    private PrepareUtil prepareUtil;

    @Test
    void startPreparePhaseOneToNormalizeAddressQueue() {
        when(config.isPrepareTwoPhases()).thenReturn(true);

        final PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("requestId")
                .correlationId(null)
                .build();

        assertThatCode(() -> prepareUtil.startPreparePhaseOne(event)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToNormalizeAddressQueue(event);


    }

    @Test
    void startPreparePhaseOneToRequestsQueueInPrepareSyncFlow() {
        when(config.isPrepareTwoPhases()).thenReturn(false);

        final PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("requestId")
                .iun("iun")
                .isAddressRetry(false)
                .attemptRetry(0)
                .correlationId(null)
                .build();

        var prepareAsyncRequest = new PrepareAsyncRequest("requestId", "iun", false, 0);

        assertThatCode(() -> prepareUtil.startPreparePhaseOne(event)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToInternalQueue(prepareAsyncRequest);

    }

    @Test
    void startPreparePhaseOneToRequestsQueueInNationaRegistriesAsyncFlow() {
        when(config.isPrepareTwoPhases()).thenReturn(false);

        Address address = new Address();
        address.setAddress("via Roma 12");

        final PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("requestId")
                .correlationId("correlationId")
                .address(address)
                .build();


        var prepareAsyncRequest = new PrepareAsyncRequest("requestId", "correlationId", address);

        assertThatCode(() -> prepareUtil.startPreparePhaseOne(event)).doesNotThrowAnyException();

        verify(sqsSender, times(1)).pushToInternalQueue(prepareAsyncRequest);

    }

}
