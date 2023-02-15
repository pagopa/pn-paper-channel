package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.mapper.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
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
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.validator.SendRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_IN_PROCESSING;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;

@Slf4j
@Service
public class PaperMessagesServiceImpl extends BaseService implements PaperMessagesService {

    @Autowired
    private AddressDAO addressDAO;

    @Autowired
    private ExternalChannelClient externalChannelClient;
    @Autowired
    private PnPaperChannelConfig pnPaperChannelConfig;

    public PaperMessagesServiceImpl(PnAuditLogBuilder auditLogBuilder, NationalRegistryClient nationalRegistryClient,
                                    RequestDeliveryDAO requestDeliveryDAO, SqsSender sqsSender, CostDAO costDAO) {

        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsSender);
    }

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

                    if (StringUtils.equals(entity.getStatusCode(), StatusDeliveryEnum.IN_PROCESSING.getCode())) {
                        return Mono.error(new PnGenericException(DELIVERY_REQUEST_IN_PROCESSING, DELIVERY_REQUEST_IN_PROCESSING.getMessage(), HttpStatus.CONFLICT));
                    }

                    // verifico se è la prima volta che viene invocata
                    if (StringUtils.equals(entity.getStatusCode(), StatusDeliveryEnum.TAKING_CHARGE.getCode())) {
                        entity.setStatusCode(StatusDeliveryEnum.READY_TO_SEND.getCode());
                        entity.setStatusDetail(StatusDeliveryEnum.READY_TO_SEND.getDescription());
                        entity.setStatusDate(DateUtils.formatDate(new Date()));
                    }

                    List<AttachmentInfo> attachments = entity.getAttachments().stream().map(AttachmentMapper::fromEntity).toList();
                    Address address = saveAddresses(sendRequest);
                    return super.calculator(attachments, address, sendRequest.getProductType()).map(value -> value);
                })
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)))
                .zipWhen(entityAndAmount ->{
                    sendRequest.setRequestId(requestId.concat(Const.RETRY).concat("0"));
                    pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                    return this.externalChannelClient.sendEngageRequest(sendRequest)
                                    .then(Mono.defer(() -> {
                                        pnLogAudit.addsSuccessSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                                        return this.requestDeliveryDAO.updateData(entityAndAmount.getT1()).map(item -> item);
                                    }))
                                    .onErrorResume(ex -> {
                                        pnLogAudit.addsFailSend(sendRequest.getIun(), String.format("prepare requestId = %s, trace_id = %s  request to External Channel", sendRequest.getRequestId(), MDC.get(MDC_TRACE_ID_KEY)));
                                        return Mono.error(ex);
                                    });
                }, (entityAndAmount, none) -> entityAndAmount.getT2())
                .map(amount -> {
                    log.info("Amount: {} for requestId {}", amount, requestId);
                    SendResponse sendResponse = new SendResponse();
                    sendResponse.setAmount(amount.intValue()*100);
                    return sendResponse;
                });
    }

    private Address saveAddresses(SendRequest sendRequest) {
        Address address = AddressMapper.fromAnalogToAddress(sendRequest.getReceiverAddress(), sendRequest.getProductType().getValue(),Const.EXECUTION);
        PnAddress addressEntity = AddressMapper.toEntity(address,sendRequest.getRequestId(), pnPaperChannelConfig);
        //save receiver address
        addressDAO.create(addressEntity);
        if (sendRequest.getSenderAddress() != null) {
            Address senderAddress = AddressMapper.fromAnalogToAddress(sendRequest.getSenderAddress(), sendRequest.getProductType().getValue(),Const.EXECUTION);
            addressDAO.create(AddressMapper.toEntity(senderAddress,sendRequest.getRequestId(), AddressTypeEnum.SENDER_ADDRES,pnPaperChannelConfig));
        }
        if (sendRequest.getArAddress() != null) {
            Address arAddress = AddressMapper.fromAnalogToAddress(sendRequest.getArAddress(), sendRequest.getProductType().getValue(),Const.EXECUTION);
            addressDAO.create(AddressMapper.toEntity(arAddress,sendRequest.getRequestId(), AddressTypeEnum.AR_ADDRESS, pnPaperChannelConfig));
        }

        return address;
    }

    @Override
    public Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        prepareRequest.setRequestId(requestId);

        if (StringUtils.isEmpty(prepareRequest.getRelatedRequestId())){
            log.info("First attempt requestId {}", requestId);
            //case of 204
            return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                    .flatMap(entity -> {
                        PrepareRequestValidator.compareRequestEntity(prepareRequest, entity, true);
                        return addressDAO.findByRequestId(requestId)
                                .map(address-> PreparePaperResponseMapper.fromResult(entity,address))
                                .switchIfEmpty(Mono.just(PreparePaperResponseMapper.fromResult(entity,null)));
                    })
                    .switchIfEmpty(Mono.defer(() -> saveRequestAndAddress(prepareRequest, null, prepareRequest.getReceiverAddress())
                            .flatMap(response -> {
                                PrepareAsyncRequest request = new PrepareAsyncRequest(requestId, response.getIun(), false, 0);
                                this.sqsSender.pushToInternalQueue(request);
                                throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(prepareRequest.getRequestId()));
                            }))
                    );
        }

        log.info("Second attempt requestId {}", requestId);
        return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRelatedRequestId())
                .flatMap(oldEntity -> {
                    prepareRequest.setRequestId(oldEntity.getRequestId());
                    PrepareRequestValidator.compareRequestEntity(prepareRequest, oldEntity, false);
                    prepareRequest.setRequestId(requestId);

                    return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                            .flatMap(newEntity -> {
                                if (newEntity == null) {
                                    log.info("New attempt");
                                    return Mono.empty();
                                }
                                log.info("Attempt already exist");
                                PrepareRequestValidator.compareRequestEntity(prepareRequest, newEntity, false);
                                return addressDAO.findByRequestId(requestId)
                                        .map(address-> PreparePaperResponseMapper.fromResult(newEntity,address))
                                        .switchIfEmpty(Mono.just(PreparePaperResponseMapper.fromResult(newEntity,null)));
                            })
                            .switchIfEmpty(Mono.defer(()-> saveRequestAndAddress(prepareRequest, oldEntity.getAddressHash(), prepareRequest.getDiscoveredAddress())
                                    .flatMap(response -> {

                                        log.info("Start call national");
                                        this.finderAddressFromNationalRegistries(
                                                (MDC.get(MDC_TRACE_ID_KEY) == null ? UUID.randomUUID().toString() : MDC.get(MDC_TRACE_ID_KEY)),
                                                response.getRequestId(),
                                                response.getRelatedRequestId(),
                                                response.getFiscalCode(),
                                                response.getReceiverType(),
                                                response.getIun(), 0);
                                        throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(prepareRequest.getRequestId()));
                                    })
                            ));
                })
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<SendEvent> retrievePaperSendRequest(String requestId) {
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> {


                    if (entity.getStatusCode().equals(StatusDeliveryEnum.TAKING_CHARGE.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.IN_PROCESSING.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.PAPER_CHANNEL_NEW_REQUEST.getCode())) {
                        return Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND));
                    }

                    return addressDAO.findByRequestId(requestId).map(address -> address)
                            .switchIfEmpty(Mono.just(new PnAddress()));
                })
                .map(entityAndAddress -> SendEventMapper.fromResult(entityAndAddress.getT1(),entityAndAddress.getT2()))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    private Mono<PnDeliveryRequest> saveRequestAndAddress(PrepareRequest prepareRequest, String oldAddress, AnalogAddress address){
        PnDeliveryRequest pnDeliveryRequest = RequestDeliveryMapper.toEntity(prepareRequest);
        pnDeliveryRequest.setHashOldAddress(oldAddress);
        PnAddress addressEntity = null;

       if (address != null) {
           Address mapped = AddressMapper.fromAnalogToAddress(address, null, Const.PREPARE);
           pnDeliveryRequest.setAddressHash(mapped.convertToHash());
           addressEntity = AddressMapper.toEntity(mapped, prepareRequest.getRequestId(), pnPaperChannelConfig);
           pnDeliveryRequest.setProductType(getProposalProductType(mapped, pnDeliveryRequest.getProposalProductType()));
       }

        return requestDeliveryDAO.createWithAddress(pnDeliveryRequest, addressEntity);
    }
}
