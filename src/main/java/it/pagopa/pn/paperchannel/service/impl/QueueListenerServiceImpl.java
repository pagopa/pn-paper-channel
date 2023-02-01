package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_LISTENER_EXCEPTION;

@Slf4j
@Service
public class QueueListenerServiceImpl extends BaseService implements QueueListenerService {
    @Autowired
    private PaperResultAsyncService paperResultAsyncService;
    @Autowired
    private PaperAsyncService paperAsyncService;

    public QueueListenerServiceImpl(PnAuditLogBuilder auditLogBuilder,
                                    RequestDeliveryDAO requestDeliveryDAO,
                                    CostDAO costDAO,
                                    NationalRegistryClient nationalRegistryClient,
                                    SqsSender sqsSender) {
        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsSender);
    }

    @Override
    public void internalListener(PrepareAsyncRequest body, int attempt) {
        Mono.just(body)
                .flatMap(prepareRequest -> {
                    prepareRequest.setAttemptRetry(attempt);
                    return this.paperAsyncService.prepareAsync(prepareRequest);
                })
                .doOnSuccess(resultFromAsync ->
                        log.info("End of prepare async internal")
                )
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    throw new PnGenericException(PREPARE_ASYNC_LISTENER_EXCEPTION, PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage());
                })
                .block();
    }

    @Override
    public void nationalRegistriesResponseListener(AddressSQSMessageDto body) {
        Mono.just(body)
                .map(dto -> {
                    if (dto==null || StringUtils.isBlank(dto.getCorrelationId())) throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
                    String correlationId = dto.getCorrelationId();
                    Address address = null;
                    if (dto.getPhysicalAddress()!=null)
                        address= AddressMapper.fromNationalRegistry(dto.getPhysicalAddress());
                    return Tuples.of(correlationId, address);
                })
                .doOnSuccess(correlationAndAddress -> {
                    PrepareAsyncRequest prepareAsyncRequest =
                            new PrepareAsyncRequest(null, correlationAndAddress.getT1(), correlationAndAddress.getT2(), false, 0);
                    this.sqsSender.pushToInternalQueue(prepareAsyncRequest);
                })
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
                })
                .block();
    }

    @Override
    public void nationalRegistriesErrorListener(NationalRegistryError data, int attempt) {
        Mono.just(data)
            .doOnSuccess(nationalRegistryError -> {
                log.info("Called national Registries");
                this.finderAddressFromNationalRegistries(
                        nationalRegistryError.getRequestId(),
                        nationalRegistryError.getRelatedRequestId(),
                        nationalRegistryError.getFiscalCode(),
                        nationalRegistryError.getReceiverType(),
                        nationalRegistryError.getIun(),
                        attempt
                );
            })
            .doOnError(throwable -> {
                log.error(throwable.getMessage());
                throw new PnGenericException(PREPARE_ASYNC_LISTENER_EXCEPTION, PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage());
            })
            .block();
    }

    @Override
    public void externalChannelListener(SingleStatusUpdateDto data, int attempt) {
        Mono.just(data)
                .flatMap(request -> this.paperResultAsyncService.resultAsyncBackground(request, attempt))
                .doOnSuccess(resultFromAsync -> log.info("End of external Channel result"))
                .onErrorResume(ex -> {
                    log.error(ex.getMessage());
                    throw new PnGenericException(EXTERNAL_CHANNEL_LISTENER_EXCEPTION, EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage());
                })
                .block();
    }

}
