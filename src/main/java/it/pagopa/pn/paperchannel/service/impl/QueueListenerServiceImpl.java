package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@Service
@CustomLog
public class QueueListenerServiceImpl extends BaseService implements QueueListenerService {
    @Autowired
    private PaperResultAsyncService paperResultAsyncService;
    @Autowired
    private PaperAsyncService paperAsyncService;
    @Autowired
    private AddressDAO addressDAO;
    String SQS_SENDER = "SQS SENDER";
    String SQS_SENDER_DESCRIPTION = "Pushing prepare event.";

    String NATIONAL_REGISTRY = "NATIONAL REGISTRY";
    String NATIONAL_REGISTRY_DESCRIPTION = "Retrieve the address.";

    public QueueListenerServiceImpl(PnAuditLogBuilder auditLogBuilder,
                                    RequestDeliveryDAO requestDeliveryDAO,
                                    CostDAO costDAO,
                                    NationalRegistryClient nationalRegistryClient,
                                    SqsSender sqsSender) {
        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsSender);
    }

    @Override
    public void internalListener(PrepareAsyncRequest body, int attempt) {
        String PROCESS_NAME = "InternalListener";
        log.logStartingProcess(PROCESS_NAME);
        Mono.just(body)
                .flatMap(prepareRequest -> {
                    prepareRequest.setAttemptRetry(attempt);
                    return this.paperAsyncService.prepareAsync(prepareRequest);
                })
                .doOnSuccess(resultFromAsync ->{
                            log.info("End of prepare async internal");
                            log.logEndingProcess(PROCESS_NAME);
                        }
                )
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    throw new PnGenericException(PREPARE_ASYNC_LISTENER_EXCEPTION, PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage());
                })
                .block();
    }

    @Override
    public void nationalRegistriesResponseListener(AddressSQSMessageDto body) {
        String PROCESS_NAME = "National Registries Response Listener";
        log.logStartingProcess(PROCESS_NAME);
        log.info("Received message from National Registry queue");
        Mono.just(body)
                .map(msg -> {
                    if (msg==null || StringUtils.isBlank(msg.getCorrelationId())) throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
                    else return msg;
                })
                .zipWhen(msgDto -> {
                    String correlationId = msgDto.getCorrelationId();
                    log.info("Received message from National Registry queue with correlationId "+correlationId);
                    return requestDeliveryDAO.getByCorrelationId(correlationId)
                            .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage())));
                })
                .doOnNext(addressAndEntity -> nationalResolveLogAudit(addressAndEntity.getT2(), addressAndEntity.getT1()))
                .flatMap(addressAndEntity -> {
                    AddressSQSMessageDto addressFromNational = addressAndEntity.getT1();
                    PnDeliveryRequest entity = addressAndEntity.getT2();


                    if (addressFromNational.getPhysicalAddress() != null) {
                        Address address = AddressMapper.fromNationalRegistry(addressFromNational.getPhysicalAddress());
                        return this.retrieveRelatedAddress(entity.getRelatedRequestId(), address)
                                .map(updateAddress -> new PrepareAsyncRequest(entity.getCorrelationId(), updateAddress));
                    }

                    return Mono.just(new PrepareAsyncRequest(addressAndEntity.getT2().getCorrelationId(), null));
                })
                .flatMap(prepareRequest -> {
                    log.logInvokingAsyncExternalService(SQS_SENDER, SQS_SENDER_DESCRIPTION, prepareRequest.getRequestId());
                    this.sqsSender.pushToInternalQueue(prepareRequest);
                    log.logEndingProcess(PROCESS_NAME);
                    return Mono.empty();
                })
                .block();
    }

    @Override
    public void nationalRegistriesErrorListener(NationalRegistryError data, int attempt) {
        String PROCESS_NAME = "National Registries Error Listener";
        log.logStartingProcess(PROCESS_NAME);
        Mono.just(data)
                .doOnSuccess(nationalRegistryError -> {
                    log.info("Called national Registries");
                    log.logInvokingAsyncExternalService(NATIONAL_REGISTRY,NATIONAL_REGISTRY_DESCRIPTION, nationalRegistryError.getRequestId());
                    this.finderAddressFromNationalRegistries(
                            nationalRegistryError.getCorrelationId(),
                            nationalRegistryError.getRequestId(),
                            nationalRegistryError.getRelatedRequestId(),
                            nationalRegistryError.getFiscalCode(),
                            nationalRegistryError.getReceiverType(),
                            nationalRegistryError.getIun(),
                            attempt
                    );
                    log.logEndingProcess(PROCESS_NAME);
                })
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    throw new PnGenericException(PREPARE_ASYNC_LISTENER_EXCEPTION, PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage());
                })
                .block();
    }

    @Override
    public void externalChannelListener(SingleStatusUpdateDto data, int attempt) {
        String PROCESS_NAME = "External Channel Listener";
        log.logStartingProcess(PROCESS_NAME);
        Mono.just(data)
                .flatMap(request -> this.paperResultAsyncService.resultAsyncBackground(request, attempt))
                .doOnSuccess(resultFromAsync -> {
                    log.info("End of external Channel result");
                    log.logEndingProcess(PROCESS_NAME);
                })
                .onErrorResume(ex -> {
                    log.error(ex.getMessage());
                    throw new PnGenericException(EXTERNAL_CHANNEL_LISTENER_EXCEPTION, EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage());
                })
                .block();
    }


    private Mono<Address> retrieveRelatedAddress(String relatedRequestId, Address fromNationalRegistry){
        return addressDAO.findByRequestId(relatedRequestId)
                .map(address -> {
                    fromNationalRegistry.setFullName(address.getFullName());
                    return fromNationalRegistry;
                }).switchIfEmpty(Mono.just(fromNationalRegistry));

    }


    private void nationalResolveLogAudit(PnDeliveryRequest entity, AddressSQSMessageDto address) {
        if (StringUtils.isEmpty(address.getError())) {
            pnLogAudit.addsSuccessResolveService(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, traceId = %s Response OK from National Registry service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            entity.getCorrelationId()));
        } else {
            pnLogAudit.addsFailResolveService(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, traceId = %s Response KO from National Registry service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            entity.getCorrelationId()));
        }
    }

}
