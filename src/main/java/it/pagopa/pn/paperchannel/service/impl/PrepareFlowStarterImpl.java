package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import it.pagopa.pn.paperchannel.service.PrepareFlowStarter;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

/**
 * Utility class that manages the PREPARE phase.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrepareFlowStarterImpl implements PrepareFlowStarter {

    private final SqsSender sqsSender;
    private final PnPaperChannelConfig config;

    /**
     * Starts the asynchronous PREPARE flow from the synchronous PREPARE
     * @param deliveryRequest Paper Channel entity representing a shipping request
     * @param clientId Possible identifier of the client who called the PREPARE
     */
    public void startPreparePhaseOneFromPrepareSync(PnDeliveryRequest deliveryRequest, String clientId) {

        if(isPrepareTwoPhases()) {
            log.debug("Internal event from PREPARE sync to pn-paper-normalize-address queue");
            var prepareNormalizeAddressEvent = this.buildEventFromPrepareSync(deliveryRequest, clientId);
            this.sqsSender.pushToNormalizeAddressQueue(prepareNormalizeAddressEvent);
        }

        else {
            log.debug("Internal event from PREPARE sync to pn-paper_channel_requests queue");
            var request = new PrepareAsyncRequest(deliveryRequest.getRequestId(), deliveryRequest.getIun(), false, 0);
            this.sqsSender.pushToInternalQueue(request);
        }
    }

    /**
     * Starts the asynchronous PREPARE flow from the national registries flow
     * @param deliveryRequest Paper Channel entity representing a shipping request
     * @param nationalRegistriesAddress Possible address retrieved from national records
     */
    public void startPreparePhaseOneFromNationalRegistriesFlow(PnDeliveryRequest deliveryRequest, @Nullable Address nationalRegistriesAddress) {

        if(isPrepareTwoPhases()) {
            log.debug("Internal event from national registries flow to pn-paper-normalize-address queue");
            var prepareNormalizeAddressEvent = this.buildEventFromNationalRegistriesFlow(deliveryRequest, nationalRegistriesAddress);
            this.sqsSender.pushToNormalizeAddressQueue(prepareNormalizeAddressEvent);
        }

        else {
            log.debug("Internal event from national registries flow to pn-paper_channel_requests queue");
            var request = new PrepareAsyncRequest(deliveryRequest.getRequestId(), deliveryRequest.getCorrelationId(), nationalRegistriesAddress);
            this.sqsSender.pushToInternalQueue(request);
        }
    }

    @Override
    public void redrivePreparePhaseOneAfterNationalRegistryError(NationalRegistryError entity, int attemptRetry) {
        if(isPrepareTwoPhases()) {
            this.sqsSender.redrivePreparePhaseOneAfterError(entity, attemptRetry, NationalRegistryError.class);
        }
        else {
            this.sqsSender.pushInternalError(entity, attemptRetry, NationalRegistryError.class);
        }
    }

    @Override
    public void redrivePreparePhaseOneAfterAddressManagerError(PnDeliveryRequest deliveryRequest, int attemptRetry) {
        if(isPrepareTwoPhases()) {
            PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                    .requestId(deliveryRequest.getRequestId())
                    .correlationId(deliveryRequest.getCorrelationId())
                    .iun(deliveryRequest.getIun())
                    .isAddressRetry(true)
                    .attempt(attemptRetry)
                    .build();
            this.sqsSender.redrivePreparePhaseOneAfterError(event, event.getAttempt(), PrepareNormalizeAddressEvent.class);
        }
        else {
            PrepareAsyncRequest queueModel = new PrepareAsyncRequest();
            queueModel.setIun(deliveryRequest.getIun());
            queueModel.setRequestId(deliveryRequest.getRequestId());
            queueModel.setCorrelationId(deliveryRequest.getCorrelationId());
            queueModel.setAddressRetry(true);
            queueModel.setAttemptRetry(attemptRetry);
            this.sqsSender.pushInternalError(queueModel, queueModel.getAttemptRetry(), PrepareAsyncRequest.class);
        }
    }

    private boolean isPrepareTwoPhases() {
        return Boolean.TRUE.equals(config.isPrepareTwoPhases());
    }

    /**
     * Method that builds the message payload to invoke PREPARE phase 1 from the synchronous PREPARE flows of ATTEMPT 0 and ATTEMPT 1.
     * @param deliveryRequest Paper Channel entity representing a shipping request
     * @param clientId Possible identifier of the client who called the PREPARE
     * @return The payload of the input message of PREPARE phase 1
     */
    private PrepareNormalizeAddressEvent buildEventFromPrepareSync(PnDeliveryRequest deliveryRequest, String clientId) {
        return PrepareNormalizeAddressEvent.builder()
                .requestId(deliveryRequest.getRequestId())
                .iun(deliveryRequest.getIun())
                .isAddressRetry(false)
                .attempt(0)
                .clientId(clientId)
                .build();

    }

    /**
     * Method that builds the message payload to invoke PREPARE phase 1 from the asynchronous response of national registries.
     * @param deliveryRequest Paper Channel entity representing a shipping request
     * @param nationalRegistriesAddress Possible address retrieved from national records
     * @return The payload of the input message of PREPARE phase 1
     */
    private PrepareNormalizeAddressEvent buildEventFromNationalRegistriesFlow(PnDeliveryRequest deliveryRequest, @Nullable Address nationalRegistriesAddress) {
        return PrepareNormalizeAddressEvent.builder()
                .requestId(deliveryRequest.getRequestId())
                .correlationId(deliveryRequest.getCorrelationId())
                .address(nationalRegistriesAddress)
                .build();

    }

}
