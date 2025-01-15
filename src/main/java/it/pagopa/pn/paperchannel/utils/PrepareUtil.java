package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

/**
 * Utility class that manages the PREPARE phase.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrepareUtil {

    private final SqsSender sqsSender;
    private final PnPaperChannelConfig config;


    /**
     * Method that builds the message payload to invoke PREPARE phase 1 from the synchronous PREPARE flows of ATTEMPT 0 and ATTEMPT 1.
     * @param deliveryRequest Paper Channel entity representing a shipping request
     * @param clientId Possible identifier of the client who called the PREPARE
     * @return The payload of the input message of PREPARE phase 1
     */
    public static PrepareNormalizeAddressEvent buildEventFromPrepareSync(PnDeliveryRequest deliveryRequest, String clientId) {
        return PrepareNormalizeAddressEvent.builder()
                .requestId(deliveryRequest.getRequestId())
                .iun(deliveryRequest.getIun())
                .isAddressRetry(false)
                .attemptRetry(0)
                .clientId(clientId)
                .build();

    }

    /**
     * Method that builds the message payload to invoke PREPARE phase 1 from the asynchronous response of national registries.
     * @param deliveryRequest Paper Channel entity representing a shipping request
     * @param nationalRegistriesAddress Possible address retrieved from national records
     * @return The payload of the input message of PREPARE phase 1
     */
    public static PrepareNormalizeAddressEvent buildEventFromNationalRegistriesFlow(PnDeliveryRequest deliveryRequest, @Nullable Address nationalRegistriesAddress) {
        return PrepareNormalizeAddressEvent.builder()
                .requestId(deliveryRequest.getRequestId())
                .correlationId(deliveryRequest.getCorrelationId())
                .address(nationalRegistriesAddress)
                .build();

    }


    public void startPreparePhaseOne(PrepareNormalizeAddressEvent prepareNormalizeAddressEvent) {

        if(Boolean.TRUE.equals(config.isPrepareTwoPhases())) {
            log.debug("Internal event to pn-paper-normalize-address queue");
            this.sqsSender.pushToNormalizeAddressQueue(prepareNormalizeAddressEvent);
        }

        else {
            log.debug("Internal event to pn-paper_channel_requests queue");
            startPrepareOldFlow(prepareNormalizeAddressEvent);
        }
    }

    private void startPrepareOldFlow(PrepareNormalizeAddressEvent prepareNormalizeAddressEvent) {
        PrepareAsyncRequest request;

        if(StringUtils.isNotBlank(prepareNormalizeAddressEvent.getCorrelationId())) {
            log.debug("Old PREPARE async flow from national registries");
            request = new PrepareAsyncRequest(prepareNormalizeAddressEvent.getRequestId(), prepareNormalizeAddressEvent.getCorrelationId(), prepareNormalizeAddressEvent.getAddress());
        }
        else {
            log.debug("Old PREPARE async flow from PREPARE sync");
            request = new PrepareAsyncRequest(prepareNormalizeAddressEvent.getRequestId(), prepareNormalizeAddressEvent.getIun(), false, 0);
        }

        this.sqsSender.pushToInternalQueue(request);
    }

}
