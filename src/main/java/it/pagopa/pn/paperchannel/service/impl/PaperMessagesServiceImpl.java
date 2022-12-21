package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.mapper.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.validator.SendRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

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
    public Mono<SendResponse> executionPaper(String requestId, SendRequest sendRequest) {
        return this.requestDeliveryDAO.getByRequestId(sendRequest.getRequestId())
                .flatMap(entity -> {
                    SendRequestValidator.compareRequestEntity(sendRequest,entity);
                    if (StringUtils.isNotBlank(sendRequest.getReceiverAddress().getCap())) {
                        return getAmount(entity.getAttachments(),sendRequest.getReceiverAddress().getCap(), sendRequest.getProductType())
                                .map(item -> item);
                    }
                    return getZone(sendRequest.getReceiverAddress().getCountry())
                            .flatMap(zone -> getAmount(entity.getAttachments(), zone, sendRequest.getProductType()).map(item -> item));

                }).map(amount -> {
                    //TODO Aggiungere push su coda di external channel
                    SendResponse sendResponse = new SendResponse();
                    sendResponse.setAmount(amount.intValue());
                    return sendResponse;
                });

    }

    private Mono<String> getZone(String country) {
        return Mono.just("ZONA_1");
    }
    private Mono<Double> getPriceAttachments(List<PnAttachmentInfo> pnAttachmentInfos, Double priceForAAr){
        return Flux.fromStream(pnAttachmentInfos.stream())
                .map(attachmentInfo -> attachmentInfo.getNumberOfPage() * priceForAAr)
                .reduce(0.0, Double::sum);
    }
    private Mono<Contract> getContract(String capOrZone, String registerLetter) {
        return Mono.just(new Contract(5.0, 10.0));
    }
    private Mono<Double> getAmount(List<PnAttachmentInfo> attachments, String capOrZone, ProductTypeEnum registerLetter ){
        return getContract(capOrZone,registerLetter.getValue())
                .flatMap(contract -> getPriceAttachments(attachments, contract.getPricePerPage())
                        .map(priceForPages -> Double.sum(contract.getPrice(), priceForPages))
                );

    }

    @Override
    public Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        log.debug("Start preparePaperSync with requestId {}", requestId);
        prepareRequest.setRequestId(requestId);


        if (prepareRequest.getRelatedRequestId() == null){
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
