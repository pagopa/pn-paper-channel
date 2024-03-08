package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.service.SecondAttemptFlowService;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ATTEMPT_ADDRESS_NATIONAL_REGISTRY;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DISCARD_NOTIFICATION;

@Slf4j
public class PstSecondAttemptFlowService extends SecondAttemptFlowService {

    private static final String DISCOVERED_ADDRESS_NAME = "Discovered Address";

    public PstSecondAttemptFlowService(AddressManagerClient addressManagerClient, PnPaperChannelConfig pnPaperChannelConfig) {
        super(addressManagerClient, pnPaperChannelConfig);
    }

    @Override
    public Mono<Address> handleSecondAttempt(PnDeliveryRequest pnDeliveryRequest, Address secondAttemptAddress, Address firstAttemptAddress) {
        log.info("flowPostmanAddress for requestId {}", pnDeliveryRequest.getRequestId());
        return super.handleSecondAttempt(pnDeliveryRequest, secondAttemptAddress, firstAttemptAddress);
    }

    @Override
    public void handleError(PnDeliveryRequest pnDeliveryRequest, DeduplicatesResponseDto deduplicatesResponse, Address discoveredAddress) {
        handleDeduplicationError(DISCARD_NOTIFICATION, deduplicatesResponse.getError(), discoveredAddress, pnDeliveryRequest.getRequestId());
    }

    @Override
    public void handleSameAddresses(PnDeliveryRequest pnDeliveryRequest, Address secondAttemptAddress) {
        throw new PnAddressFlowException(ATTEMPT_ADDRESS_NATIONAL_REGISTRY);
    }

    @Override
    protected RuntimeException throwExceptionToContinueFlowAfterError(Address addressFailed) {
        return new PnAddressFlowException(ATTEMPT_ADDRESS_NATIONAL_REGISTRY);
    }

    @Override
    public String retrieveCorrelationId(PnDeliveryRequest deliveryRequest) {
        return Utility.buildPostmanAddressCorrelationId(deliveryRequest.getRequestId());
    }

    @Override
    public String getAddressName() {
        return DISCOVERED_ADDRESS_NAME;
    }


}
