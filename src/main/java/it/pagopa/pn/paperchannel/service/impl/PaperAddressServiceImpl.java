package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.PaperAddressService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@Slf4j
@Service
public class PaperAddressServiceImpl extends BaseService implements PaperAddressService {
    @Autowired
    private PnPaperChannelConfig paperProperties;
    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private AddressManagerClient addressManagerClient;
    @Autowired
    private PaperRequestErrorDAO paperRequestErrorDAO;

    public PaperAddressServiceImpl(PnAuditLogBuilder auditLogBuilder, NationalRegistryClient nationalRegistryClient,
                                   RequestDeliveryDAO requestDeliveryDAO, SqsSender sqsQueueSender, CostDAO costDAO ) {
        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsQueueSender);
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
                .flatMap(receiverAddress -> {
                    logAuditBefore("prepare requestId = %s, relatedRequestId = %s Is National Registry Address present ?", deliveryRequest);

                    // Only discovered address is null and retrieved national registry address
                    if (StringUtils.isNotBlank(deliveryRequest.getCorrelationId())){
                        if (fromNationalRegistry == null) {
                            logAuditSuccess("prepare requestId = %s, relatedRequestId = %s National Registry Address is null", deliveryRequest);
                            return Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage()));
                        }
                        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s National Registry Address is present", deliveryRequest);
                        return flowNationalRegistry(deliveryRequest, fromNationalRegistry, receiverAddress);
                    }
                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s National Registry not present", deliveryRequest);

                    // Only discovered address is present and check discovered address
                    logAuditBefore("prepare requestId = %s, relatedRequestId = %s Is Second attempt ?", deliveryRequest);
                    if (StringUtils.isNotBlank(deliveryRequest.getRelatedRequestId())){
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
                                .flatMap(discoveredAddress -> flowPostmanAddress(deliveryRequest, discoveredAddress, receiverAddress));
                    }


                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Is not second attempt and use receiver address", deliveryRequest);
                    return Mono.just(receiverAddress);
                })
                .onErrorResume(PnAddressFlowException.class, ex -> {
                    if (ex.getExceptionType() == ATTEMPT_ADDRESS_NATIONAL_REGISTRY){
                        this.finderAddressFromNationalRegistries(
                                (MDC.get(MDC_TRACE_ID_KEY) == null ? UUID.randomUUID().toString() : MDC.get(MDC_TRACE_ID_KEY)),
                                deliveryRequest.getRequestId(),
                                deliveryRequest.getRelatedRequestId(),
                                deliveryRequest.getFiscalCode(),
                                deliveryRequest.getReceiverType(),
                                deliveryRequest.getIun(), 0);
                        return Mono.error(ex);
                    }
                    if (ex.getExceptionType() == DISCARD_NOTIFICATION){
                        return changeStateDeliveryRequest(deliveryRequest, StatusDeliveryEnum.DISCARD_NOTIFICATION)
                                .flatMap(entity ->
                                        traceError(deliveryRequest.getRequestId(), ex.getMessage(), ex.getExceptionType().getTitle())
                                            .then(Mono.defer(() -> Mono.error(ex)))
                                );
                    }
                    if (ex.getExceptionType() == ADDRESS_MANAGER_ERROR){
                        queueModel.setIun(deliveryRequest.getIun());
                        queueModel.setRequestId(deliveryRequest.getRequestId());
                        queueModel.setCorrelationId(deliveryRequest.getCorrelationId());
                        queueModel.setAddressRetry(true);
                        this.sqsSender.pushInternalError(queueModel, queueModel.getAttemptRetry(), PrepareAsyncRequest.class);
                    }
                    return Mono.error(ex);
                });
    }



    private Mono<Address> flowPostmanAddress(PnDeliveryRequest deliveryRequest, Address discovered, Address firstAttempt){
        logAuditBefore("prepare requestId = %s, relatedRequestId = %s Discovered and First address is Equals ?", deliveryRequest);

        return this.addressManagerClient.deduplicates(UUID.randomUUID().toString(), firstAttempt, discovered)
                .flatMap(deduplicatesResponse -> {
                    if (StringUtils.isNotBlank(deduplicatesResponse.getError())){
                        if (StringUtils.equalsIgnoreCase(paperProperties.getOriginalPostmanAddressUsageMode(), Const.PAPERSEND)){
                            logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Deduplicate response have an error and send discovered",deliveryRequest);
                            return Mono.just(discovered);
                        } else if (StringUtils.equalsIgnoreCase(paperProperties.getOriginalPostmanAddressUsageMode(), Const.DISCARDNOTIFICATION)) {
                            logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Deduplicate response have an error and discard notification",deliveryRequest);
                            return Mono.error(new PnAddressFlowException(DISCARD_NOTIFICATION));
                        }
                        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Deduplicate response have a error and properties usage mode incompatible type",deliveryRequest);
                        return Mono.error(new PnInternalException(INVALID_VALUE_FROM_PROPS.getTitle(), "ERROR_POSTMAN_ADDRESS_USAGE_MODE"));
                    }
                    if (Boolean.TRUE.equals(deduplicatesResponse.getEqualityResult())) {
                        logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Discovered address is equals previous address",deliveryRequest);
                        return Mono.error(new PnAddressFlowException(ATTEMPT_ADDRESS_NATIONAL_REGISTRY));
                    }
                    logAuditSuccess("prepare requestId = %s, relatedRequestId = %s Discovered address is not equals previous address", deliveryRequest);
                    return checkAndParseNormalizedAddress(deduplicatesResponse.getNormalizedAddress(), discovered, deliveryRequest.getRequestId());
                });
    }



    private Mono<Address> flowNationalRegistry(PnDeliveryRequest pnDeliveryRequest, Address fromNationalRegistries, Address firstAttempt){
        return addressManagerClient.deduplicates(pnDeliveryRequest.getCorrelationId(), firstAttempt, fromNationalRegistries)
                .flatMap(deduplicateResponse -> {
                    if (Boolean.TRUE.equals(deduplicateResponse.getEqualityResult())) {
                        pnLogAudit.addsSuccessResolveLogic(
                                pnDeliveryRequest.getIun(),
                                String.format("prepare requestId = %s, relatedRequestId = %s National Registry Address is equals previous address",
                                        pnDeliveryRequest.getRequestId(),
                                        pnDeliveryRequest.getRelatedRequestId())
                        );
                        return Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage()));
                    }
                    if (deduplicateResponse.getError() != null){
                        log.error("Response from address manager {} with request id {}", deduplicateResponse.getError(), pnDeliveryRequest.getRequestId());
                        return Mono.error(new PnGenericException(ADDRESS_MANAGER_ERROR, deduplicateResponse.getError()));
                    }
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

    private Mono<Address> checkAndParseNormalizedAddress(AnalogAddressDto normalizedAddress, Address older, String requestId){
        if (normalizedAddress == null) {
            log.error("Response from address manager have a address null {}", requestId);
            return Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage()));
        }
        Address address = AddressMapper.fromAnalogAddressManager(normalizedAddress) ;
        address.setFullName(older.getFullName());
        address.setNameRow2(older.getNameRow2());
        return Mono.just(address);
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

    private Mono<Void> traceError(String requestId, String error, String flowType){
        return this.paperRequestErrorDAO.created(requestId, error, flowType)
                .then();
    }

    private Mono<PnDeliveryRequest> changeStateDeliveryRequest(PnDeliveryRequest deliveryRequest, StatusDeliveryEnum status) {
        return super.requestDeliveryDAO.getByRequestId(deliveryRequest.getRequestId()).flatMap(
                entity -> {
                    RequestDeliveryMapper.changeState(
                            entity,
                            status.getCode(),
                            status.getDescription(),
                            status.getDetail(),
                            null,
                            null
                    );
                    return this.requestDeliveryDAO.updateData(entity);
                });
    }

}
