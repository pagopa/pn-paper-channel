package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnUntracebleException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.service.SecondAttemptFlowService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ADDRESS_MANAGER_ERROR;

@Slf4j
public class NrgSecondAttemptFlowService extends SecondAttemptFlowService {

    private static final String NATIONAL_REGISTRIES_ADDRESS_NAME = "National Registry Address";

    public NrgSecondAttemptFlowService(AddressManagerClient addressManagerClient, PnPaperChannelConfig pnPaperChannelConfig) {
        super(addressManagerClient, pnPaperChannelConfig);
    }

    @Override
    public Mono<Address> handleSecondAttempt(PnDeliveryRequest pnDeliveryRequest, Address secondAttemptAddress, Address firstAttemptAddress) {
        log.info("flowNationalRegistry for requestId {}", pnDeliveryRequest.getRequestId());
        return super.handleSecondAttempt(pnDeliveryRequest, secondAttemptAddress, firstAttemptAddress);
    }

    @Override
    public void handleError(PnDeliveryRequest pnDeliveryRequest, DeduplicatesResponseDto deduplicatesResponse, Address secondAttemptAddress) {
        handleDeduplicationError(ADDRESS_MANAGER_ERROR, deduplicatesResponse.getError(), secondAttemptAddress, pnDeliveryRequest.getRequestId());
    }

    @Override
    public void handleSameAddresses(PnDeliveryRequest pnDeliveryRequest, Address secondAttemptAddress) {
        KOReason koReason = new KOReason(FailureDetailCodeEnum.D02, secondAttemptAddress);
        throw new PnUntracebleException(koReason);
        //Indirizzo coincidenti = D02
    }

    @Override
    protected RuntimeException throwExceptionToContinueFlowAfterError(Address addressFailed) {
        KOReason koReason = new KOReason(FailureDetailCodeEnum.D01, addressFailed);
        return new PnUntracebleException(koReason);
    }

    @Override
    public String retrieveCorrelationId(PnDeliveryRequest deliveryRequest) {
        return deliveryRequest.getCorrelationId();
    }

    @Override
    public String getAddressName() {
        return NATIONAL_REGISTRIES_ADDRESS_NAME;
    }
}
