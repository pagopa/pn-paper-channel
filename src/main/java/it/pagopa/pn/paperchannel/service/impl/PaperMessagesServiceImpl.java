package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.PreparePaperResponseMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.PrepareEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.StringUtil;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;

@Slf4j
@Service
public class PaperMessagesServiceImpl implements PaperMessagesService {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private NationalRegistryClient nationalRegistryClient;
    @Autowired
    private PrepareAsyncServiceImpl prepareAsyncService;
    @Autowired
    private SqsSender sqsSender;


    @Override
    public Mono<PrepareEvent> retrievePaperPrepareRequest(String requestId) {
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> addressDAO.findByRequestId(requestId).map(address -> address))
                .map(entityAndAddress -> PrepareEventMapper.fromResult(entityAndAddress.getT1(),entityAndAddress.getT2()))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage())));
    }

    @Override
    public Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        log.debug("Start preparePaperSync with requestId {}", requestId);
        prepareRequest.setRequestId(requestId);

        if (StringUtils.isEmpty(prepareRequest.getRelatedRequestId())){
            log.debug("First attempt");
            //case of 204
            return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                    .flatMap(entity -> {
                        PrepareRequestValidator.compareRequestEntity(prepareRequest, entity);
                        return addressDAO.findByRequestId(requestId)
                                .map(address-> PreparePaperResponseMapper.fromResult(entity,address))
                                .switchIfEmpty(Mono.just(PreparePaperResponseMapper.fromResult(entity,null)));
                    })
                    .switchIfEmpty(Mono.defer(() -> saveRequestAndAddress(prepareRequest, null)
                            .flatMap(response -> Mono.empty()))
                    );

        }

        log.debug("Second attemp");
        return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRelatedRequestId())
                .flatMap(oldEntity -> {
                    prepareRequest.setRequestId(oldEntity.getRequestId());
                    PrepareRequestValidator.compareRequestEntity(prepareRequest, oldEntity);
                    prepareRequest.setRequestId(requestId);
                    return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                            .flatMap(newEntity -> {
                                if (newEntity == null) {
                                    log.debug("New attempt");
                                    return Mono.empty();
                                }
                                log.debug("Attempt already exist");
                                PrepareRequestValidator.compareRequestEntity(prepareRequest, newEntity);
                                return addressDAO.findByRequestId(requestId)
                                        .map(address-> PreparePaperResponseMapper.fromResult(newEntity,address))
                                        .switchIfEmpty(Mono.just(PreparePaperResponseMapper.fromResult(newEntity,null)));
                            })
                            .switchIfEmpty(Mono.defer(()-> finderAddressAndSave(prepareRequest).flatMap(response -> Mono.empty())));
                })
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage())));

    }


    private Mono<PnDeliveryRequest> finderAddressAndSave(PrepareRequest prepareRequest){
        return this.nationalRegistryClient.finderAddress(prepareRequest.getReceiverFiscalCode(), prepareRequest.getReceiverType())
                .flatMap(response -> {
                    log.debug("Response from national registry {}", response.getCorrelationId());
                    return saveRequestAndAddress(prepareRequest, response.getCorrelationId())
                            .map(savedEntity -> savedEntity);
                });
    }


    private Mono<PnDeliveryRequest> saveRequestAndAddress(PrepareRequest prepareRequest, String correlationId){
        PnDeliveryRequest pnDeliveryRequest = RequestDeliveryMapper.toEntity(prepareRequest, correlationId);
        Address address = AddressMapper.fromAnalogToAddress(prepareRequest.getDiscoveredAddress());
        pnDeliveryRequest.setAddressHash(address.convertToHash());

        PnAddress addressEntity = null;
        if (correlationId == null){
            addressEntity = AddressMapper.toEntity(address, prepareRequest.getRequestId());
        }

        return requestDeliveryDAO.createWithAddress(pnDeliveryRequest, addressEntity)
                .map(entity -> {
                    // Case of 204
                    throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(prepareRequest.getRequestId()));
                });
    }
}
