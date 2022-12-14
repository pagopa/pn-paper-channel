package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.mapper.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.validator.SendRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_IN_PROCESSING;

@Slf4j
@Service
public class PaperMessagesServiceImpl extends BaseService implements PaperMessagesService {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private NationalRegistryClient nationalRegistryClient;
    @Autowired
    private ExternalChannelClient externalChannelClient;
    @Autowired
    private PrepareAsyncServiceImpl prepareAsyncService;
    @Autowired
    private SqsSender sqsSender;


    @Override
    public Mono<PrepareEvent> retrievePaperPrepareRequest(String requestId) {
        log.info("Start retrieve prepare request");
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> addressDAO.findByRequestId(requestId).map(address -> address)
                            .switchIfEmpty(Mono.just(new PnAddress()))
                )
                .map(entityAndAddress -> PrepareEventMapper.fromResult(entityAndAddress.getT1(), entityAndAddress.getT2()))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<SendResponse> executionPaper(String requestId, SendRequest sendRequest) {
        log.info("Start executionPaper with requestId {}", requestId);
        sendRequest.setRequestId(requestId);

        return this.requestDeliveryDAO.getByRequestId(sendRequest.getRequestId())
                .zipWhen(entity -> {
                    SendRequestValidator.compareRequestEntity(sendRequest,entity);

                    //TODO Managed error from status code into pnDeliveryRequest
                    //verifico se la fase di prepare ?? stata completata
                    if (StringUtils.equals(entity.getStatusCode(), StatusDeliveryEnum.IN_PROCESSING.getCode())) {
                        return Mono.error(new PnGenericException(DELIVERY_REQUEST_IN_PROCESSING, DELIVERY_REQUEST_IN_PROCESSING.getMessage(), HttpStatus.CONFLICT));
                    }

                    // verifico se ?? la prima volta che viene invocata
                    if (StringUtils.equals(entity.getStatusCode(), StatusDeliveryEnum.TAKING_CHARGE.getCode())) {
                        entity.setStatusCode(StatusDeliveryEnum.READY_TO_SEND.getCode());
                        entity.setStatusDetail(StatusDeliveryEnum.READY_TO_SEND.getDescription());
                        entity.setStatusDate(DateUtils.formatDate(new Date()));
                    }

                    List<AttachmentInfo> attachments = entity.getAttachments().stream().map(AttachmentMapper::fromEntity).collect(Collectors.toList());
                    Address address = AddressMapper.fromAnalogToAddress(sendRequest.getReceiverAddress());
                    return super.calculator(attachments, address, sendRequest.getProductType()).map(value -> value);
                })
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)))
                .zipWhen(entityAndAmount ->

                        //TODO chiamare external channel solo se status == TAKING_CHARGE
                        this.externalChannelClient.sendEngageRequest(sendRequest).then(this.requestDeliveryDAO.updateData(entityAndAmount.getT1()).map(item -> item)), (entityAndAmount, none) -> entityAndAmount.getT2()
                )
                .map(amount -> {
                    log.info("Amount: {} for requestId {}", amount, requestId);
                    SendResponse sendResponse = new SendResponse();
                    sendResponse.setAmount(amount.intValue());
                    return sendResponse;
                });
    }

    @Override
    public Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        log.info("Start preparePaperSync with requestId {}", requestId);
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
                            .flatMap(response -> {
                                PrepareAsyncRequest request = new PrepareAsyncRequest(requestId, null, null);
                                this.sqsSender.pushToInternalQueue(request);
                                throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(prepareRequest.getRequestId()));
                            }))
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
                            .switchIfEmpty(Mono.defer(()-> finderAddressAndSave(prepareRequest)
                                    .flatMap(response -> {
                                        throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(prepareRequest.getRequestId()));
                                    }))
                            );

                })
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<SendEvent> retrievePaperSendRequest(String requestId) {
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> {

                    //TODO Added other status code
                    if (entity.getStatusCode().equals(StatusDeliveryEnum.TAKING_CHARGE.getCode()) ||
                        entity.getStatusCode().equals(StatusDeliveryEnum.IN_PROCESSING.getCode())) {
                        return Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND));
                    }

                    return addressDAO.findByRequestId(requestId).map(address -> address)
                            .switchIfEmpty(Mono.just(new PnAddress()));
                })
                .map(entityAndAddress -> SendEventMapper.fromResult(entityAndAddress.getT1(),entityAndAddress.getT2()))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
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
        Address address = AddressMapper.fromAnalogToAddress(prepareRequest.getReceiverAddress());
        pnDeliveryRequest.setAddressHash(address.convertToHash());

        PnAddress addressEntity = null;
        if (correlationId == null){
            addressEntity = AddressMapper.toEntity(address, prepareRequest.getRequestId());
        }

        return requestDeliveryDAO.createWithAddress(pnDeliveryRequest, addressEntity);
    }
}
