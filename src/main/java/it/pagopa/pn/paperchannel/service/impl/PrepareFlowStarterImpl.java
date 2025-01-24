package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnAddressItem;
import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.api.dto.events.PnPreparePaperchannelToDelayerPayload;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.PrepareEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.PrepareFlowStarter;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

import static it.pagopa.pn.paperchannel.utils.Const.PREFIX_REQUEST_ID_SERVICE_DESK;

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

    /**
     * Starts the asynchronous PREPARE flow from the national registries flow
     * @param deliveryRequest Paper Channel entity representing a shipping request
     * @param recipientNormalizedAddress Possible normalized address for the shipping
     */
    @Override
    public void pushPreparePhaseOneOutput(PnDeliveryRequest deliveryRequest, PnAddress recipientNormalizedAddress) {
        PnAddressItem addressItem = PnAddressItem.builder()
                .address(recipientNormalizedAddress.getAddress())
                .addressRow2(recipientNormalizedAddress.getAddressRow2())
                .cap(recipientNormalizedAddress.getCap())
                .city(recipientNormalizedAddress.getCity())
                .city2(recipientNormalizedAddress.getCity2())
                .pr(recipientNormalizedAddress.getPr())
                .country(recipientNormalizedAddress.getCountry())
                .fullName(recipientNormalizedAddress.getFullName())
                .nameRow2(recipientNormalizedAddress.getNameRow2())
                .build();

        PnPreparePaperchannelToDelayerPayload payload = PnPreparePaperchannelToDelayerPayload.builder()
                .requestId(deliveryRequest.getRequestId())
                .iun(deliveryRequest.getIun())
                .productType(deliveryRequest.getProductType())
                .recipientNormalizedAddress(addressItem)
                .build();

        this.sqsSender.pushToPaperchannelToDelayerQueue(payload);
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
    public void redrivePreparePhaseOneAfterAddressManagerError(PnDeliveryRequest deliveryRequest, int attemptRetry, Address fromNationalRegistry) {
        if(isPrepareTwoPhases()) {
            PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                    .requestId(deliveryRequest.getRequestId())
                    .correlationId(deliveryRequest.getCorrelationId())
                    .iun(deliveryRequest.getIun())
                    .address(fromNationalRegistry)
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
            queueModel.setAddress(fromNationalRegistry);
            queueModel.setAddressRetry(true);
            queueModel.setAttemptRetry(attemptRetry);
            this.sqsSender.pushInternalError(queueModel, queueModel.getAttemptRetry(), PrepareAsyncRequest.class);
        }
    }

    public void redrivePreparePhaseTwoAfterF24Flow(PnDeliveryRequest deliveryRequest) {

        if(isPrepareTwoPhases()) {
            log.debug("Internal event from f24 flow to pn-delayer_to_paperchannel queue");
            PnPrepareDelayerToPaperchannelPayload payload = PnPrepareDelayerToPaperchannelPayload.builder()
                    .requestId(deliveryRequest.getRequestId())
                    .iun(deliveryRequest.getIun())
                    .attemptRetry(0)
                    .build();
            this.sqsSender.pushToDelayerToPaperchennelQueue(payload);
        }

        else {
            log.debug("Internal event from f24 flow to pn-paper_channel_requests queue");
            PrepareAsyncRequest request = new PrepareAsyncRequest(deliveryRequest.getRequestId(), deliveryRequest.getIun(), false, 0);
            request.setF24ResponseFlow(true);
            this.sqsSender.pushToInternalQueue(request);
        }
    }

    public void pushResultPrepareEvent(PnDeliveryRequest request, Address address, String clientId, StatusCodeEnum statusCode, KOReason koReason){
        PrepareEvent prepareEvent = PrepareEventMapper.toPrepareEvent(request, address, statusCode, koReason);
        if (request.getRequestId().contains(PREFIX_REQUEST_ID_SERVICE_DESK)){
            log.info("Sending event to EventBridge: {}", prepareEvent);
            this.sqsSender.pushPrepareEventOnEventBridge(clientId, prepareEvent);
            return;
        }
        log.info("Sending event to delivery-push: {}", prepareEvent);
        this.sqsSender.pushPrepareEvent(prepareEvent);
    }

    public void redrivePreparePhaseTwoAfterF24Error(F24Error f24Error) {

        if(isPrepareTwoPhases()) {
            log.info("Attempting F24 to pushing to pn-delayer_to_paperchannel queue, payload={}", f24Error);
            this.sqsSender.pushF24ErrorDelayerToPaperChannelQueue(f24Error);
        }

        else {
            log.info("Attempting to pushing to internal payload={}", f24Error);
            sqsSender.pushInternalError(f24Error, f24Error.getAttempt(), F24Error.class);
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
