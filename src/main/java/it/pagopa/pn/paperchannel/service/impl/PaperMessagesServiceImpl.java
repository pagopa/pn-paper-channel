package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.PreparePaperResponseMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.RetrivePrepareResponseMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;

@Slf4j
@Service
public class PaperMessagesServiceImpl implements PaperMessagesService {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private NationalRegistryClient nationalRegistryClient;
    @Autowired
    private PrepareAsyncServiceImpl prepareAsyncService;
    @Autowired
    private SqsSender sqsSender;
    @Autowired
    private PrepareRequestValidator prepareRequestValidator;

    @Override
    public Mono<SendEvent> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        log.info("Start preparePaperSync with requestId {}", requestId);
        prepareRequest.setRequestId(requestId);
        return requestDeliveryDAO.getByRequestId(requestId)
                // Case of 409
                .map(entity -> prepareRequestValidator.compareRequestEntity(prepareRequest, entity))
                // Case of 200
                .map(PreparePaperResponseMapper::fromResult)
                .onErrorResume(PnGenericException.class, ex -> {
                    if (ex.getExceptionType() == DELIVERY_REQUEST_NOT_EXIST){
                        log.info("Delivery request");

                        return getAddress(prepareRequest)
                                .flatMap(address -> {
                                    if(address==null){
                                        return nationalRegistryClient.finderAddress(prepareRequest.getReceiverFiscalCode(),prepareRequest.getReceiverType())
                                                .flatMap(addressOKDto -> saveRequestDeliveryEntity(prepareRequest,null,addressOKDto.getCorrelationId())
                                                        .flatMap(entity -> Mono.empty()));
                                    }
                                    return saveRequestDeliveryEntity(prepareRequest,address,null)
                                            .flatMap(entity -> Mono.empty());
                                });
                    }
                    if (ex.getExceptionType() == DIFFERENT_DATA_REQUEST) {

                        return Mono.error(ex);
                    }
                    return Mono.error(ex);
                });

    }

    @Override
    public Mono<PrepareEvent> retrivePaperPrepareRequest(String requestId) {
        return requestDeliveryDAO.getByRequestId(requestId)
                .map(RetrivePrepareResponseMapper::fromResult);
    }

    private Mono<PnDeliveryRequest> saveRequestDeliveryEntity(PrepareRequest prepareRequest, Address address, String correlationId){
        return requestDeliveryDAO.create(RequestDeliveryMapper.toEntity(prepareRequest, correlationId))
                .map(entity -> {
                    // Case of 204
                    log.info("Entity creata");
                    if( address != null){
                        Mono.just("")
                                .publishOn(Schedulers.parallel())
                                .flatMap(item -> prepareAsyncService.prepareAsync(prepareRequest.getRequestId(),correlationId,address))
                                .subscribe(new SubscriberPrepare(sqsSender, requestDeliveryDAO, prepareRequest.getRequestId(), correlationId));
                    }
                    throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(prepareRequest.getRequestId()));
                });
    }


    private Mono<Address> getAddress(PrepareRequest prepareRequest){
        if (prepareRequest.getReceiverAddress() != null){
            return Mono.just(AddressMapper.fromAnalogToAddress(prepareRequest.getReceiverAddress()));
        }
        if (prepareRequest.getDiscoveredAddress() != null){
            return Mono.just(AddressMapper.fromAnalogToAddress(prepareRequest.getDiscoveredAddress()));
        }
        return  Mono.justOrEmpty(java.util.Optional.<Address>empty());
    }
}
