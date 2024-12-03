package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnUntracebleException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.service.PaperAddressService;
import it.pagopa.pn.paperchannel.service.SecondAttemptFlowHandlerFactory;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.utils.Utility.logAuditBeforeLogic;
import static it.pagopa.pn.paperchannel.utils.Utility.logAuditSuccessLogic;

@Slf4j
@Service
public class PaperAddressServiceImpl extends BaseService implements PaperAddressService {

    private final AddressDAO addressDAO;
    private final SecondAttemptFlowHandlerFactory secondAttemptFlowHandlerFactory;


    public PaperAddressServiceImpl(NationalRegistryClient nationalRegistryClient,
                                   RequestDeliveryDAO requestDeliveryDAO, SqsSender sqsQueueSender, CostDAO costDAO,
                                   AddressDAO addressDAO, SecondAttemptFlowHandlerFactory secondAttemptFlowHandlerFactory) {
        super(requestDeliveryDAO, costDAO, nationalRegistryClient, sqsQueueSender);
        this.addressDAO = addressDAO;
        this.secondAttemptFlowHandlerFactory = secondAttemptFlowHandlerFactory;
    }


    @Override
    public Mono<Address> getCorrectAddress(PnDeliveryRequest deliveryRequest, Address fromNationalRegistry, PrepareAsyncRequest queueModel) {

        PnLogAudit pnLogAudit = new PnLogAudit();
        logAuditBeforeLogic("prepare requestId = %s, relatedRequestId = %s Is Receiver Address First Attempt present ?", deliveryRequest, pnLogAudit);
        var requestIdFirstAttempt = Utility.getRequestIdFirstAttempt(deliveryRequest);
        return this.addressDAO.findByRequestId(requestIdFirstAttempt, AddressTypeEnum.RECEIVER_ADDRESS)
                .switchIfEmpty(Mono.defer(() -> {
                    logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s Receiver address First Attempt is not present on DB", deliveryRequest, pnLogAudit);
                    log.error("Receiver Address for {} request id not found on DB", deliveryRequest.getRequestId());
                    throw new PnGenericException(ADDRESS_NOT_EXIST, ADDRESS_NOT_EXIST.getMessage());
                }))
                .doOnNext(receiverAddressFirstAttempt -> logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s Receiver address First Attempt is present on DB", deliveryRequest, pnLogAudit))
                .map(AddressMapper::toDTO)
                .flatMap(receiverAddressFirstAttempt -> chooseAddress(deliveryRequest, fromNationalRegistry, receiverAddressFirstAttempt))
                .onErrorResume(PnAddressFlowException.class, ex -> handlePnAddressFlowException(ex, deliveryRequest, queueModel));
    }

    private Mono<Address> chooseAddress(PnDeliveryRequest deliveryRequest, Address fromNationalRegistry, Address addressFromFirstAttempt) {

        PnLogAudit pnLogAudit = new PnLogAudit();

        // Only discovered address is null and retrieved national registry address
        if (StringUtils.isNotBlank(deliveryRequest.getCorrelationId())){
            logAuditBeforeLogic("prepare requestId = %s, relatedRequestId = %s Is National Registry Address present ?", deliveryRequest, pnLogAudit);
            log.debug("[{}] getAddressFromNationalRegistry flow", deliveryRequest.getRequestId());
            return getAddressFromNationalRegistry(deliveryRequest, fromNationalRegistry, addressFromFirstAttempt);
        }
        else {
            // Only discovered address is present and check discovered address
            logAuditBeforeLogic("prepare requestId = %s, relatedRequestId = %s Is Second attempt ?", deliveryRequest, pnLogAudit);
            if (StringUtils.isNotBlank(deliveryRequest.getRelatedRequestId())){
                log.debug("[{}] getAddressFromDiscoveredAddress flow", deliveryRequest.getRequestId());
                return getAddressFromDiscoveredAddress(deliveryRequest, addressFromFirstAttempt);
            }
            else {
                //primo tentativo
                logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s Is not second attempt and use receiver address", deliveryRequest, pnLogAudit);
                return Mono.just(addressFromFirstAttempt);
            }
        }
    }

    private Mono<Address> getAddressFromNationalRegistry(PnDeliveryRequest deliveryRequest, Address fromNationalRegistry, Address addressFromFirstAttempt) {

        PnLogAudit pnLogAudit = new PnLogAudit();

        if (fromNationalRegistry == null) {
            logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s National Registry Address is null", deliveryRequest, pnLogAudit);
            KOReason koReason = new KOReason(FailureDetailCodeEnum.D00, null);
            return Mono.error(new PnUntracebleException(koReason));
            //Indirizzo non trovato = D00 - da verificare in un caso reale
        }
        logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s National Registry Address is present", deliveryRequest, pnLogAudit);

        return secondAttemptFlowHandlerFactory.getSecondAttemptFlowService(SecondAttemptFlowHandlerFactory.FlowType.NATIONAL_REGISTY_FLOW)
                .handleSecondAttempt(deliveryRequest, fromNationalRegistry, addressFromFirstAttempt);
    }

    private Mono<Address> getAddressFromDiscoveredAddress(PnDeliveryRequest deliveryRequest, Address addressFromFirstAttempt) {

        PnLogAudit pnLogAudit = new PnLogAudit();

        logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s Is Second attempt check discovered address", deliveryRequest, pnLogAudit);
        logAuditBeforeLogic("prepare requestId = %s, relatedRequestId = %s Is Discovered address present ?", deliveryRequest, pnLogAudit);
        return this.addressDAO.findByRequestId(deliveryRequest.getRequestId(), AddressTypeEnum.DISCOVERED_ADDRESS)
                .switchIfEmpty(Mono.defer(() -> {
                    logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s discovered address is not present on DB", deliveryRequest, pnLogAudit);
                    log.error("Discovered Address for {} request id not found on DB", deliveryRequest.getRequestId());
                    throw new PnGenericException(ADDRESS_NOT_EXIST, ADDRESS_NOT_EXIST.getMessage());
                }))
                .doOnNext(pnAddress -> logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s discovered address is present on DB", deliveryRequest, pnLogAudit))
                .map(AddressMapper::toDTO)
                .flatMap(discoveredAddress -> secondAttemptFlowHandlerFactory.getSecondAttemptFlowService(SecondAttemptFlowHandlerFactory.FlowType.POSTMAN_FLOW)
                        .handleSecondAttempt(deliveryRequest, discoveredAddress, addressFromFirstAttempt));

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

}
