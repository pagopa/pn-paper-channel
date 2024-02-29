package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.service.PaperAddressService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.utils.DeduplicateErrorConst.*;

@Slf4j
@Service
public class PaperAddressServiceImpl extends BaseService implements PaperAddressService {
    private final PnPaperChannelConfig paperProperties;
    private final AddressDAO addressDAO;
    private final AddressManagerClient addressManagerClient;


    public PaperAddressServiceImpl(PnAuditLogBuilder auditLogBuilder, NationalRegistryClient nationalRegistryClient,
                                   RequestDeliveryDAO requestDeliveryDAO, SqsSender sqsQueueSender, CostDAO costDAO,
                                   PnPaperChannelConfig paperProperties, AddressDAO addressDAO, AddressManagerClient addressManagerClient) {
        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsQueueSender);
        this.paperProperties = paperProperties;
        this.addressDAO = addressDAO;
        this.addressManagerClient = addressManagerClient;
    }


    @Override
    public Mono<Address> getCorrectAddress(PnDeliveryRequest deliveryRequest, Address fromNationalRegistry, PrepareAsyncRequest queueModel) {
        logAuditBefore("prepare requestId = %s, relatedRequestId = %s Is Receiver Address present ?", deliveryRequest);

        return this.addressDAO.findByRequestId(deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS)
                .switchIfEmpty(Mono.defer(() -> {
                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Receiver address is not present on DB", deliveryRequest);
                    log.error("Receiver Address for {} request id not found on DB", deliveryRequest.getRequestId());
                    throw new PnGenericException(ADDRESS_NOT_EXIST, ADDRESS_NOT_EXIST.getMessage());
                }))
                .doOnNext(pnAddress -> logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Receiver address is present on DB", deliveryRequest))
                .map(AddressMapper::toDTO)
                .flatMap(receiverAddress -> chooseAddress(deliveryRequest, fromNationalRegistry, receiverAddress))
                .onErrorResume(PnAddressFlowException.class, ex -> handlePnAddressFlowException(ex, deliveryRequest, queueModel));
    }

    private Mono<Address> chooseAddress(PnDeliveryRequest deliveryRequest, Address fromNationalRegistry, Address addressFromFirstAttempt) {
        logAuditBefore("prepare requestId = %s, relatedRequestId = %s Is National Registry Address present ?", deliveryRequest);
        // Only discovered address is null and retrieved national registry address
        if (StringUtils.isNotBlank(deliveryRequest.getCorrelationId())){
            log.debug("[{}] getAddressFromNationalRegistry flow", deliveryRequest.getRequestId());
            return getAddressFromNationalRegistry(deliveryRequest, fromNationalRegistry, addressFromFirstAttempt);
        }
        else {
            logAuditSuccess("prepare requestId = %s, relatedRequestId = %s National Registry not present", deliveryRequest);
            // Only discovered address is present and check discovered address
            logAuditBefore("prepare requestId = %s, relatedRequestId = %s Is Second attempt ?", deliveryRequest);
            if (StringUtils.isNotBlank(deliveryRequest.getRelatedRequestId())){
                log.debug("[{}] getAddressFromDiscoveredAddress flow", deliveryRequest.getRequestId());
                return getAddressFromDiscoveredAddress(deliveryRequest, addressFromFirstAttempt);
            }
            else {
                //primo tentativo
                logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Is not second attempt and use receiver address", deliveryRequest);
                return Mono.just(addressFromFirstAttempt);
            }
        }
    }

    private Mono<Address> getAddressFromNationalRegistry(PnDeliveryRequest deliveryRequest, Address fromNationalRegistry, Address addressFromFirstAttempt) {
        if (fromNationalRegistry == null) {
            logAuditSuccess("prepare requestId = %s, relatedRequestId = %s National Registry Address is null", deliveryRequest);
            KOReason koReason = new KOReason(FailureDetailCodeEnum.D00, null);
            return Mono.error(new PnUntracebleException(koReason));
            //Indirizzo non trovato = D00 - da verificare in un caso reale
        }
        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s National Registry Address is present", deliveryRequest);
        return flowNationalRegistry(deliveryRequest, fromNationalRegistry, addressFromFirstAttempt);
    }

    private Mono<Address> getAddressFromDiscoveredAddress(PnDeliveryRequest deliveryRequest, Address addressFromFirstAttempt) {
        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Is Second attempt check discovered address", deliveryRequest);
        logAuditBefore("prepare requestId = %s, relatedRequestId = %s Is Discovered address present ?", deliveryRequest);
        return this.addressDAO.findByRequestId(deliveryRequest.getRequestId(), AddressTypeEnum.DISCOVERED_ADDRESS)
                .switchIfEmpty(Mono.defer(() -> {
                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s discovered address is not present on DB", deliveryRequest);
                    log.error("Discovered Address for {} request id not found on DB", deliveryRequest.getRequestId());
                    throw new PnGenericException(ADDRESS_NOT_EXIST, ADDRESS_NOT_EXIST.getMessage());
                }))
                .doOnNext(pnAddress -> logAuditSuccess("prepare requestId = %s, relatedRequestId = %s discovered address is present on DB", deliveryRequest))
                .map(AddressMapper::toDTO)
                .flatMap(discoveredAddress -> flowPostmanAddress(deliveryRequest, discoveredAddress, addressFromFirstAttempt));

    }



    private Mono<Address> flowPostmanAddress(PnDeliveryRequest deliveryRequest, Address discovered, Address firstAttempt){
        log.info("flowPostmanAddress for requestId {}", deliveryRequest.getRequestId());
        logAuditBefore("prepare requestId = %s, relatedRequestId = %s Discovered and First address is Equals ?", deliveryRequest);
        var correlationId = Utility.buildPostmanAddressCorrelationId(deliveryRequest.getRequestId());
        return this.addressManagerClient.deduplicates(correlationId, firstAttempt, discovered)
                .flatMap(deduplicatesResponse -> {
                    logAuditBefore("prepare requestId = %s, relatedRequestId = %s Deduplicates service has DeduplicatesResponse.error empty ?", deliveryRequest);
                    if (StringUtils.isNotBlank(deduplicatesResponse.getError())){
                        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Deduplicate response have an error", deliveryRequest);
                        return handleDeduplicationError(DISCARD_NOTIFICATION, deduplicatesResponse.getError(), discovered, deliveryRequest.getRequestId());
                    }

                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Deduplicate service has DeduplicatesResponse.error is empty",deliveryRequest);

                    if (Boolean.TRUE.equals(deduplicatesResponse.getEqualityResult())) {
                        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Discovered address is equals previous address",deliveryRequest);
                        return Mono.error(new PnAddressFlowException(ATTEMPT_ADDRESS_NATIONAL_REGISTRY));
                        // questa eccezione attiva il flusso di NR
                    }
                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Discovered address is not equals previous address", deliveryRequest);
                    return checkAndParseNormalizedAddress(deduplicatesResponse.getNormalizedAddress(), discovered, deliveryRequest.getRequestId());
                });
    }

    private Mono<Address> flowNationalRegistry(PnDeliveryRequest pnDeliveryRequest, Address fromNationalRegistries, Address firstAttempt){
        log.info("flowNationalRegistry for requestId {}", pnDeliveryRequest.getRequestId());

        return addressManagerClient.deduplicates(pnDeliveryRequest.getCorrelationId(), firstAttempt, fromNationalRegistries)
                .flatMap(deduplicateResponse -> {
                    logAuditBefore("prepare requestId = %s, relatedRequestId = %s Deduplicates service has DeduplicatesResponse.error empty ?", pnDeliveryRequest);
                    if (Boolean.TRUE.equals(deduplicateResponse.getEqualityResult())) {
                        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Deduplicate service has DeduplicatesResponse.error is empty",pnDeliveryRequest);

                        pnLogAudit.addsSuccessResolveLogic(
                                pnDeliveryRequest.getIun(),
                                String.format("prepare requestId = %s, relatedRequestId = %s National Registry Address is equals previous address",
                                        pnDeliveryRequest.getRequestId(),
                                        pnDeliveryRequest.getRelatedRequestId())
                        );
                        KOReason koReason = new KOReason(FailureDetailCodeEnum.D02, fromNationalRegistries);
                        return Mono.error(new PnUntracebleException(koReason));
                        //Indirizzo coincidenti = D02
                    }
                    if (deduplicateResponse.getError() != null){
                        log.error("Response from address manager {} with request id {}", deduplicateResponse.getError(), pnDeliveryRequest.getRequestId());
                        return handleDeduplicationError(ADDRESS_MANAGER_ERROR, deduplicateResponse.getError(), fromNationalRegistries, pnDeliveryRequest.getRequestId());
                    }
                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Deduplicate service has DeduplicatesResponse.error is empty",pnDeliveryRequest);

                    pnLogAudit.addsSuccessResolveLogic(
                            pnDeliveryRequest.getIun(),
                            String.format("prepare requestId = %s, relatedRequestId = %s National Registry Address is not equals previous address",
                                    pnDeliveryRequest.getRequestId(),
                                    pnDeliveryRequest.getRelatedRequestId())
                    );
                    AnalogAddressDto addressFromManager = deduplicateResponse.getNormalizedAddress();
                    return checkAndParseNormalizedAddress(addressFromManager, firstAttempt, pnDeliveryRequest.getRequestId());
                });
    }

    private Mono<Address> handleDeduplicationError(ExceptionTypeEnum exceptionType, String errorCode, Address addressFailed, String requestId) {
        return switch (errorCode) {
            case PNADDR001, PNADDR002 ->
                    //Indirizzo diverso - Normalizzazione KO = D01 (con configurazione)
                    Mono.error(manageErrorD001(paperProperties, exceptionType, errorCode, addressFailed, requestId));
            case PNADDR999 -> Mono.error(new PnAddressFlowException(ADDRESS_MANAGER_ERROR));
            default -> Mono.error(new PnGenericException(RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION, RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION.getMessage()));
        };
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

    private <T> Mono<T> handlePnAddressFlowException(PnAddressFlowException ex, PnDeliveryRequest deliveryRequest, PrepareAsyncRequest queueModel) {
        if (ex.getExceptionType() == ATTEMPT_ADDRESS_NATIONAL_REGISTRY){
            this.finderAddressFromNationalRegistries(
                    deliveryRequest.getRequestId(),
                    deliveryRequest.getRelatedRequestId(),
                    deliveryRequest.getFiscalCode(),
                    deliveryRequest.getReceiverType(),
                    deliveryRequest.getIun(), 0);
            return Mono.error(ex);
        }
        if (ex.getExceptionType() == ADDRESS_MANAGER_ERROR){
            queueModel.setIun(deliveryRequest.getIun());
            queueModel.setRequestId(deliveryRequest.getRequestId());
            queueModel.setCorrelationId(deliveryRequest.getCorrelationId());
            queueModel.setAddressRetry(true);
            this.sqsSender.pushInternalError(queueModel, queueModel.getAttemptRetry(), PrepareAsyncRequest.class);
        }
        return Mono.error(ex);
    }

    private void logAuditSuccess(String message, PnDeliveryRequest deliveryRequest){
        pnLogAudit.addsSuccessResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }

    private void logAuditBefore(String message, PnDeliveryRequest deliveryRequest){
        pnLogAudit.addsBeforeResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }

    private Throwable manageErrorD001(PnPaperChannelConfig config, ExceptionTypeEnum exceptionType,
                                                          String errorCode, Address addressFailed, String requestId) {

        boolean isSendD01ToDeliveryPush = PNADDR001.equals(errorCode) ? config.isPnaddr001sendD01ToDeliveryPush() :
                config.isPnaddr002sendD01ToDeliveryPush();

        if(isSendD01ToDeliveryPush) {
            log.debug("[{}] SendD01ToDeliveryPush is enabled, send D001 event to delivery push", requestId);
            return throwD001(addressFailed);
        }
        else {
            log.warn("[{}] D001 Event discarded ", requestId);
            return new PnGenericException(exceptionType, errorCode);
        }
    }

    private Throwable throwD001(Address addressFailed) {
        KOReason koReason = new KOReason(FailureDetailCodeEnum.D01, addressFailed);
        return new PnUntracebleException(koReason);
    }


}
