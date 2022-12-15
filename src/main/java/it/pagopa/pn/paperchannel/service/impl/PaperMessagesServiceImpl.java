package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.PreparePaperResponseMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.RetrivePrepareResponseMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.AddressEntity;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private SafeStorageClient safeStorageClient;
    @Autowired
    private NationalRegistryClient nationalRegistryClient;
    @Autowired
    private PrepareAsyncServiceImpl prepareAsyncService;

    @Override
    public Mono<SendEvent> preparePaperSync(String requestId, PrepareRequest prepareRequest){

        return requestDeliveryDAO.getByRequestId(requestId)
                // Case of 409
                //.map(entity -> compareRequestEntity(prepareRequest, entity))
                // Case of 200,
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


    private Mono<RequestDeliveryEntity> saveRequestDeliveryEntity(PrepareRequest prepareRequest, Address address, String correlationId){
        return requestDeliveryDAO.create(RequestDeliveryMapper.toEntity(prepareRequest, correlationId))
                .map(entity -> {
                    // Case of 204
                    log.info("Entity creata");
                    if( address != null){
                        Mono.just("")
                                .publishOn(Schedulers.parallel())
                                .flatMap(item -> prepareAsyncService.prepareAsync(prepareRequest.getRequestId(),correlationId,address))
                                .subscribe(new SubscriberPrepare(null, requestDeliveryDAO, prepareRequest.getRequestId(), correlationId));
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


    private RequestDeliveryEntity compareRequestEntity(PrepareRequest prepareRequest, RequestDeliveryEntity requestDeliveryEntity) {
        // se request id uguali e lo status dell'entity è NATIONAL_REGISTRY_WAITING vuol dire che abbiamo già elaborato la richiesta

        //se uno di questi campi è null lanciare errore: requestid -fiscalcode -reciveredtipe -address -producttype -printtype -attachmenturls

        if((prepareRequest.getRequestId().equals(requestDeliveryEntity.getRequestId()) &&
                (!(prepareRequest.getReceiverFiscalCode().equals(requestDeliveryEntity.getFiscalCode()))) ||
                (!(prepareRequest.getProductType().equals(requestDeliveryEntity.getRegisteredLetterCode()))))){

            //caso in cui recivered address è popolato mentre discovered address no
            if(((prepareRequest.getReceiverAddress()!=null && prepareRequest.getDiscoveredAddress()==null) ||
                    (prepareRequest.getReceiverAddress()==null && prepareRequest.getDiscoveredAddress()!=null))
                //&&
                //TODO riattivare quando presente address checkAddressInfo(prepareRequest,requestDeliveryEntity
            ){
                //recivered address diverso da quello precedentemente ricevuto
                throw new PnGenericException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT);
            }
            //caso in cui sono entrambi null l'indirizzo viene recuperato dal national registry
            if(prepareRequest.getReceiverAddress()==null && prepareRequest.getDiscoveredAddress()==null){
                throw new PnGenericException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT);
            }
        }
        return requestDeliveryEntity;
    }

    private boolean checkAddressInfo(PrepareRequest prepareRequest, AddressEntity addressEntity){

        return (!prepareRequest.getReceiverAddress().getAddress().equals(addressEntity.getAddress()) ||
                !prepareRequest.getReceiverAddress().getFullname().equals(addressEntity.getFullName()) ||
                !prepareRequest.getReceiverAddress().getNameRow2().equals(addressEntity.getNameRow2()) ||
                !prepareRequest.getReceiverAddress().getAddressRow2().equals(addressEntity.getAddressRow2()) ||
                !prepareRequest.getReceiverAddress().getCap().equals(addressEntity.getCap()) ||
                !prepareRequest.getReceiverAddress().getCity().equals(addressEntity.getCity()) ||
                !prepareRequest.getReceiverAddress().getCity2().equals(addressEntity.getCity2()) ||
                !prepareRequest.getReceiverAddress().getPr().equals(addressEntity.getPr()) ||
                !prepareRequest.getReceiverAddress().getCountry().equals(addressEntity.getCountry()));

    }
}
