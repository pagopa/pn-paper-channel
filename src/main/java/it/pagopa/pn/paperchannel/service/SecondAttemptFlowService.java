package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnDeduplicationException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.utils.DeduplicateErrorConst.*;
import static it.pagopa.pn.paperchannel.utils.Utility.*;

@RequiredArgsConstructor
@Slf4j
public abstract class SecondAttemptFlowService {

    private final AddressManagerClient addressManagerClient;

    protected final PnPaperChannelConfig paperProperties;


    public Mono<Address> handleSecondAttempt(PnDeliveryRequest pnDeliveryRequest, Address secondAttemptAddress, Address firstAttemptAddress) {
        String correlationId = retrieveCorrelationId(pnDeliveryRequest);
        return callDeduplica(correlationId, pnDeliveryRequest, secondAttemptAddress, firstAttemptAddress)
                .doOnNext(deduplicatesResponse -> checkAndHandleErrors(pnDeliveryRequest, deduplicatesResponse, secondAttemptAddress))
                .doOnNext(deduplicatesResponse -> checkAndHandleSameAddresses(pnDeliveryRequest, deduplicatesResponse, secondAttemptAddress))
                .flatMap(deduplicatesResponse -> checkAndParseNormalizedAddress(deduplicatesResponse.getNormalizedAddress(), firstAttemptAddress, pnDeliveryRequest.getRequestId()));
    }

    public void checkAndHandleErrors(PnDeliveryRequest pnDeliveryRequest, DeduplicatesResponseDto deduplicatesResponse, Address secondAttemptAddress) {
        PnLogAudit pnLogAudit = new PnLogAudit();

        logAuditBeforeLogic("prepare requestId = %s, relatedRequestId = %s Deduplicates service has DeduplicatesResponse.error empty ?", pnDeliveryRequest, pnLogAudit);
        if (StringUtils.isNotBlank(deduplicatesResponse.getError())) {
            log.error("Response from address manager {} with request id {}", deduplicatesResponse.getError(), pnDeliveryRequest.getRequestId());
            logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s Deduplicate response have an error", pnDeliveryRequest, pnLogAudit);
            handleError(pnDeliveryRequest, deduplicatesResponse, secondAttemptAddress);
        }
        logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s Deduplicate service has DeduplicatesResponse.error is empty", pnDeliveryRequest, pnLogAudit);

    }

    public void checkAndHandleSameAddresses(PnDeliveryRequest pnDeliveryRequest, DeduplicatesResponseDto deduplicatesResponse, Address secondAttemptAddress) {
        PnLogAudit pnLogAudit = new PnLogAudit();
        logAuditBeforeLogic("prepare requestId = %s, relatedRequestId = %s " + getAddressName() + " and First address is Equals ?", pnDeliveryRequest, pnLogAudit);

        if (Boolean.TRUE.equals(deduplicatesResponse.getEqualityResult())) {
            logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s " + getAddressName() + " is equals previous address", pnDeliveryRequest, pnLogAudit);
            handleSameAddresses(pnDeliveryRequest, secondAttemptAddress);
        }

        logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s " + getAddressName() + " is not equals previous address", pnDeliveryRequest, pnLogAudit);

    }

    protected abstract void handleError(PnDeliveryRequest pnDeliveryRequest, DeduplicatesResponseDto deduplicatesResponse, Address secondAttemptAddress);

    protected abstract void handleSameAddresses(PnDeliveryRequest pnDeliveryRequest, Address secondAttemptAddress);

    protected abstract RuntimeException throwExceptionToContinueFlowAfterError(Address addressFailed);

    protected abstract String retrieveCorrelationId(PnDeliveryRequest deliveryRequest);

    protected abstract String getAddressName();


    protected Mono<DeduplicatesResponseDto> callDeduplica(String correlationId, PnDeliveryRequest pnDeliveryRequest, Address secondAttemptAddress, Address firstAttemptAddress) {
        PnLogAudit pnLogAudit = new PnLogAudit();

        var iun = pnDeliveryRequest.getIun();
        var requestId = pnDeliveryRequest.getRequestId();
        var relatedRequestId = pnDeliveryRequest.getRelatedRequestId();
        pnLogAudit.addsBeforeResolveService(iun, String.format("prepare requestId = %s, relatedRequestId= %s, correlationId = %s Request to %s", requestId, relatedRequestId, correlationId, PnLogger.EXTERNAL_SERVICES.PN_ADDRESS_MANAGER));
        return addressManagerClient.deduplicates(correlationId, firstAttemptAddress, secondAttemptAddress)
                .doOnNext(deduplicatesResponseDto -> pnLogAudit.addsSuccessResolveService(pnDeliveryRequest.getIun(),
                        String.format("prepare requestId = %s, relatedRequestId = %s, correlationId = %s Response OK from %s service",
                                pnDeliveryRequest.getRequestId(), pnDeliveryRequest.getRelatedRequestId(),
                                correlationId, PnLogger.EXTERNAL_SERVICES.PN_ADDRESS_MANAGER))
                )
                .doOnError(ex -> {
                            pnLogAudit.addsFailResolveService(iun, String.format("prepare requestId = %s, relatedRequestId = %s, correlationId = %s Response KO from %s", requestId, relatedRequestId, correlationId, PnLogger.EXTERNAL_SERVICES.PN_ADDRESS_MANAGER));
                            log.warn("Address Manager deduplicates with correlationId {} in errors {}", correlationId, ex.getMessage());
                        }
                );
    }

    private Mono<Address> checkAndParseNormalizedAddress(AnalogAddressDto normalizedAddress, Address older, String requestId){
        if (normalizedAddress == null) {
            log.error("Response from address manager have a address null, requestId: {}", requestId);
            return Mono.error(new PnGenericException(RESPONSE_NULL_FROM_DEDUPLICATION, "Response from address manager have a address null, requestId: " + requestId));
            //Indirizzo non trovato = D00 - da verificare in un caso reale
        }
        Address address = AddressMapper.fromAnalogAddressManager(normalizedAddress) ;
        address.setFullName(older.getFullName());
        address.setNameRow2(older.getNameRow2());
        return Mono.just(address);
    }

    protected void handleDeduplicationError(ExceptionTypeEnum exceptionType, String errorCode, Address addressFailed, String requestId) {
        switch (errorCode) {
            case PNADDR001, PNADDR002 ->
                //Indirizzo diverso - Normalizzazione KO = D01 (con configurazione)
                    throw manageErrorD01(paperProperties, exceptionType, errorCode, addressFailed, requestId);
            case PNADDR999 -> throw new PnAddressFlowException(ADDRESS_MANAGER_ERROR);
            default -> throw new PnGenericException(RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION, RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION.getMessage());
        }
    }

    private RuntimeException manageErrorD01(PnPaperChannelConfig config, ExceptionTypeEnum exceptionType,
                                            String errorCode, Address addressFailed, String requestId) {

        boolean isContinueFlow = PNADDR001.equals(errorCode) ? config.isPnaddr001continueFlow() :
                config.isPnaddr002continueFlow();

        if(isContinueFlow) {
            log.debug("[{}] ContinueFlow for {} is enabled, continue flow", requestId, errorCode);
            return throwExceptionToContinueFlowAfterError(addressFailed);
        }
        else { // PNADDR002
            log.debug("[{}] ContinueFlow for {} is disabled, stop flow", requestId, errorCode);
            return new PnDeduplicationException(exceptionType, errorCode, addressFailed.getCap());
        }
    }
}
