package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
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
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.Utility;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.validator.SendRequestValidator;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_IN_PROCESSING;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.READY_TO_SEND;
import static it.pagopa.pn.paperchannel.utils.Const.AAR;

@CustomLog
@Service
public class  PaperMessagesServiceImpl extends BaseService implements PaperMessagesService {

    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private ExternalChannelClient externalChannelClient;
    @Autowired
    private PnPaperChannelConfig pnPaperChannelConfig;
    @Autowired
    private PaperTenderService paperTenderService;

    String EXTERNAL_CHANNEL_DESCRIPTION = "Shipment notification service";

    public PaperMessagesServiceImpl(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO, CostDAO costDAO,
                                    NationalRegistryClient nationalRegistryClient, SqsSender sqsSender) {
        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsSender);
    }

    @Override
    public Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        prepareRequest.setRequestId(requestId);
        if (StringUtils.isEmpty(prepareRequest.getRelatedRequestId())){
            log.info("First attempt requestId {}", requestId);

            //case of 204
            log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table {}", requestId, "RequestDeliveryDynamoTable");
            return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                    .flatMap(entity -> {
                        log.info("Founded data in DynamoDB table {}", "RequestDeliveryDynamoTable");
                        PrepareRequestValidator.compareRequestEntity(prepareRequest, entity, true);
                        log.debug("Getting PnAddress with requestId {}, in DynamoDB table {}", requestId, "AddressDynamoTable");
                        return addressDAO.findByRequestId(requestId)
                                .map(address-> {
                                    log.info("Founded data in DynamoDB table {}", "AddressDynamoTable");
                                    return PreparePaperResponseMapper.fromResult(entity, address);
                                })
                                .switchIfEmpty(Mono.just(PreparePaperResponseMapper.fromResult(entity,null)));
                    })
                    .switchIfEmpty(Mono.defer(() -> saveRequestAndAddress(prepareRequest)
                            .flatMap(response -> {
                                PrepareAsyncRequest request = new PrepareAsyncRequest(requestId, response.getIun(), false, 0);
                                this.sqsSender.pushToInternalQueue(request);
                                return Mono.empty();
                            }))
                    );
        }

        log.info("Second attempt requestId {}", requestId);
        log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table {}", requestId, "RequestDeliveryDynamoTable");
        return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRelatedRequestId())
                .doOnNext(oldEntity -> {
                    log.info("Founded data in DynamoDB table {}", "RequestDeliveryDynamoTable");
                    prepareRequest.setRequestId(oldEntity.getRequestId());
                    PrepareRequestValidator.compareRequestEntity(prepareRequest, oldEntity, false);
                })
                .zipWhen(oldEntity -> {
                    log.debug("Getting PnAddress with requestId {}, in DynamoDB table {}", oldEntity.getRequestId(), "AddressDynamoTable");
                    return addressDAO.findByRequestId(oldEntity.getRequestId())
                                .map(address -> {
                                    log.info("Founded data in DynamoDB table {}", "AddressDynamoTable");
                                    log.debug("Address of the related request");
                                    log.debug(
                                            "name surname: {}, address: {}, zip: {}, foreign state: {}",
                                            LogUtils.maskGeneric(address.getFullName()),
                                            LogUtils.maskGeneric(address.getAddress()),
                                            LogUtils.maskGeneric(address.getCap()),
                                            LogUtils.maskGeneric(address.getCountry())
                                    );
                                    return address;
                                });
                }, (entity, address) -> entity)
                .flatMap(oldEntity -> {
                    prepareRequest.setRequestId(requestId);
                    log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table {}", requestId, "RequestDeliveryDynamoTable");
                    return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                            .flatMap(newEntity -> {
                                log.info("Attempt already exist for request id : {}", prepareRequest.getRequestId());
                                log.info("Founded data in DynamoDB table {}", "RequestDeliveryDynamoTable");
                                PrepareRequestValidator.compareRequestEntity(prepareRequest, newEntity, false);
                                log.debug("Getting PnAddress with requestId {}, in DynamoDB table {}", newEntity.getRequestId(), "AddressDynamoTable");
                                return addressDAO.findByRequestId(newEntity.getRequestId())
                                        .map(address-> {
                                            log.info("Founded data in DynamoDB table {}", "AddressDynamoTable");
                                            return PreparePaperResponseMapper.fromResult(newEntity, address);
                                        })
                                        .switchIfEmpty(Mono.just(PreparePaperResponseMapper.fromResult(newEntity,null)));
                            })
                            .switchIfEmpty(Mono.defer(() ->
                                    saveRequestAndAddress(prepareRequest)
                                            .flatMap(response -> {
                                                pnLogAudit.addsBeforeResolveLogic(
                                                        prepareRequest.getIun(),
                                                        String.format("prepare requestId = %s, relatedRequestId = %s Is Discovered Address present ?", requestId, prepareRequest.getRelatedRequestId())
                                                );

                                                if (prepareRequest.getDiscoveredAddress() != null) {
                                                    log.debug("Discovered address");
                                                    log.debug(
                                                            "name surname: {}, address: {}, zip: {}, foreign state: {}",
                                                            LogUtils.maskGeneric(prepareRequest.getDiscoveredAddress().getFullname()),
                                                            LogUtils.maskGeneric(prepareRequest.getDiscoveredAddress().getAddress()),
                                                            LogUtils.maskGeneric(prepareRequest.getDiscoveredAddress().getCap()),
                                                            LogUtils.maskGeneric(prepareRequest.getDiscoveredAddress().getCountry())
                                                    );
                                                    pnLogAudit.addsSuccessResolveLogic(
                                                            prepareRequest.getIun(),
                                                            String.format("prepare requestId = %s, relatedRequestId = %s Discovered Address is present", requestId, prepareRequest.getRelatedRequestId())
                                                    );

                                                    PrepareAsyncRequest request = new PrepareAsyncRequest(response.getRequestId(), response.getIun(), false, 0);
                                                    this.sqsSender.pushToInternalQueue(request);
                                                } else {
                                                    pnLogAudit.addsSuccessResolveLogic(
                                                            prepareRequest.getIun(),
                                                            String.format("prepare requestId = %s, relatedRequestId = %s Discovered Address is not present", requestId, prepareRequest.getRelatedRequestId())
                                                    );
                                                    this.finderAddressFromNationalRegistries(
                                                            (MDC.get(MDCUtils.MDC_TRACE_ID_KEY) == null ? UUID.randomUUID().toString() : MDC.get(MDCUtils.MDC_TRACE_ID_KEY)),
                                                            response.getRequestId(),
                                                            response.getRelatedRequestId(),
                                                            response.getFiscalCode(),
                                                            response.getReceiverType(),
                                                            response.getIun(), 0);
                                                }
                                                return Mono.error(new PnPaperEventException(prepareRequest.getRequestId()));
                                            })
                            ));
                })
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<PrepareEvent> retrievePaperPrepareRequest(String requestId) {
        log.info("Start retrieve prepare request {}", requestId);
        log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table {}", requestId, "RequestDeliveryDynamoTable");
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> {
                    log.info("Founded data in DynamoDB table {}", "RequestDeliveryDynamoTable");
                    log.debug("Getting PnAddress with requestId {}, in DynamoDB table {}", requestId, "AddressDynamoTable");
                    return addressDAO.findByRequestId(requestId).map(address -> {
                        log.info("Founded data in DynamoDB table {}", "AddressDynamoTable");
                        return address;
                            })
                            .switchIfEmpty(Mono.just(new PnAddress()));
                        }
                )
                .map(entityAndAddress -> PrepareEventMapper.fromResult(entityAndAddress.getT1(), entityAndAddress.getT2()))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<SendResponse> executionPaper(String requestId, SendRequest sendRequest) {
        log.info("Start executionPaper with requestId {}", requestId);
        sendRequest.setRequestId(requestId);

        log.debug("Getting data {} with requestId {} in DynamoDb table {}", "PnDeliveryRequest", requestId, "RequestDeliveryDynamoTable");
        return this.requestDeliveryDAO.getByRequestId(sendRequest.getRequestId())
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)))
                .map(entity -> {
                    log.info("Founded data in DynamoDb table {}", "RequestDeliveryDynamoTable");
                    SendRequestValidator.compareRequestEntity(sendRequest,entity);
                    if (StringUtils.equals(entity.getStatusCode(), StatusDeliveryEnum.IN_PROCESSING.getCode())) {
                       throw new PnGenericException(DELIVERY_REQUEST_IN_PROCESSING, DELIVERY_REQUEST_IN_PROCESSING.getMessage(), HttpStatus.CONFLICT);
                    }
                    log.info("RequestId - {}, Product type - {}",
                            entity.getRequestId(), entity.getProductType());
                    return entity;
                })
                .flatMap(pnDeliveryRequest -> {
                    List<AttachmentInfo> attachments = pnDeliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList();
                    Address address = saveAddresses(sendRequest);
                    log.debug(
                            "name surname: {}, address: {}, zip: {}, foreign state: {}",
                            LogUtils.maskGeneric(sendRequest.getReceiverAddress().getFullname()),
                            LogUtils.maskGeneric(sendRequest.getReceiverAddress().getAddress()),
                            LogUtils.maskGeneric(sendRequest.getReceiverAddress().getCap()),
                            LogUtils.maskGeneric(sendRequest.getReceiverAddress().getCountry())
                        );
                    String VALIDATION_NAME = "Amount calculation process";
                    log.logChecking(VALIDATION_NAME);
                    return getSendResponse(
                            address,
                            attachments,
                            sendRequest.getProductType(),
                            StringUtils.equals(sendRequest.getPrintType(), Const.BN_FRONTE_RETRO)
                    ).map(response -> {
                        log.logCheckingOutcome(VALIDATION_NAME, true);
                        return Tuples.of(response, pnDeliveryRequest, attachments, address);
                    });
                })
                .flatMap(tuple -> {
                    SendResponse sendResponse = tuple.getT1();
                    PnDeliveryRequest pnDeliveryRequest = tuple.getT2();
                    List<AttachmentInfo> attachments = tuple.getT3();
                    Address address = tuple.getT4();

                    if (StringUtils.equals(pnDeliveryRequest.getStatusCode(), StatusDeliveryEnum.TAKING_CHARGE.getCode())) {
                        RequestDeliveryMapper.changeState(
                                pnDeliveryRequest,
                                READY_TO_SEND.getCode(),
                                READY_TO_SEND.getDescription(),
                                READY_TO_SEND.getDetail(),
                                pnDeliveryRequest.getProductType(),
                                null);
                        pnDeliveryRequest.setRequestPaId(sendRequest.getRequestPaId());
                        pnDeliveryRequest.setPrintType(sendRequest.getPrintType());

                        sendRequest.setRequestId(requestId.concat(Const.RETRY).concat("0"));
                        pnLogAudit.addsBeforeSend(
                                sendRequest.getIun(),
                                String.format("prepare requestId = %s, trace_id = %s  request to External Channel",
                                        sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY))
                        );
                        return this.externalChannelClient.sendEngageRequest(sendRequest, attachments)
                                .then(Mono.defer(() -> {
                                    pnLogAudit.addsSuccessSend(sendRequest.getIun(),
                                            String.format("prepare requestId = %s, trace_id = %s  request to External Channel",
                                                    sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY))
                                    );
                                    log.debug("Updating data {} with requestId {} in DynamoDb table {}", "PnDeliveryRequest", requestId, "RequestDeliveryDynamoTable");
                                    return this.requestDeliveryDAO.updateData(pnDeliveryRequest);
                                }))
                                .map(updated -> {
                                    log.info("Updated data in DynamoDb table {}", "RequestDeliveryDynamoTable");
                                    pushDeduplicatesErrorEvent(pnDeliveryRequest, address);
                                    return sendResponse;
                                })
                                .onErrorResume(ex -> {
                                    pnLogAudit.addsFailSend(sendRequest.getIun(),
                                            String.format("prepare requestId = %s, trace_id = %s  request to External Channel",
                                                    sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY))
                                    );
                                    return Mono.error(ex);
                                });

                    }
                    return Mono.just(sendResponse);
                });
    }

    @Override
    public Mono<SendEvent> retrievePaperSendRequest(String requestId) {
        log.debug("Getting data {} with requestId {} in DynamoDb table {}", "PnDeliveryRequest", requestId, "RequestDeliveryDynamoTable");
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> {
                    log.info("Founded data in DynamoDb table {}", "RequestDeliveryDynamoTable");

                    if (entity.getStatusCode().equals(StatusDeliveryEnum.TAKING_CHARGE.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.IN_PROCESSING.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.PAPER_CHANNEL_NEW_REQUEST.getCode())) {
                        return Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND));
                    }
                    log.debug("Getting data {} with requestId {} in DynamoDb table {}", "PnAddress", requestId, "AddressDynamoTable");
                    return addressDAO.findByRequestId(requestId).map(address -> {
                                log.info("Founded data in DynamoDb table {}", "AddressDynamoTable");
                                return address;
                            })
                            .switchIfEmpty(Mono.just(new PnAddress()));
                })
                .map(entityAndAddress -> SendEventMapper.fromResult(entityAndAddress.getT1(),entityAndAddress.getT2()))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    private Mono<SendResponse> getSendResponse(Address address, List<AttachmentInfo> attachments, ProductTypeEnum productType, boolean isReversePrinter){
        return this.calculator(attachments, address, productType, isReversePrinter)
                .map(amout -> {
                    int totalPages = getNumberOfPages(attachments, isReversePrinter, true);
                    float amoutPriceFormat = Utility.getPriceFormat(amout);
                    log.debug("Amount : {}", amoutPriceFormat);
                    log.debug("Total pages : {}", totalPages);
                    SendResponse response = new SendResponse();
                    response.setAmount((int) (amoutPriceFormat*100));
                    response.setNumberOfPages(totalPages);
                    response.setEnvelopeWeight(getLetterWeight(totalPages));
                    return response;
                });

    }

    private int getLetterWeight(int  numberOfPages){
        int weightPaper = this.pnPaperChannelConfig.getPaperWeight();
        int weightLetter = this.pnPaperChannelConfig.getLetterWeight();
        return (weightPaper * numberOfPages) + weightLetter;
    }

    private Mono<Float> calculator(List<AttachmentInfo> attachments, Address address, ProductTypeEnum productType, boolean isReversePrinter){
        boolean isNational = StringUtils.isBlank(address.getCountry()) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "it") ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "italia") ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "italy");

        if (StringUtils.isNotBlank(address.getCap()) && isNational) {
            return getAmount(attachments, address.getCap(), null, getProductType(address, productType), isReversePrinter)
                    .map(item -> item);
        }
        return paperTenderService.getZoneFromCountry(address.getCountry())
                .flatMap(zone -> getAmount(attachments,null, zone, super.getProductType(address, productType), isReversePrinter).map(item -> item));

    }

    private Mono<Float> getAmount(List<AttachmentInfo> attachments, String cap, String zone, String productType, boolean isReversePrinter){
        String processName = "Get Amount";
        log.logStartingProcess(processName);
        return paperTenderService.getCostFrom(cap, zone, productType)
                .map(contract ->{
                    if (!pnPaperChannelConfig.getChargeCalculationMode().equalsIgnoreCase(AAR)){
                        Integer totPages = getNumberOfPages(attachments, isReversePrinter, false);
                        float priceTotPages = totPages * contract.getPriceAdditional();
                        log.logEndingProcess(processName);
                        return Float.sum(contract.getPrice(), priceTotPages);
                    }else{
                        log.logEndingProcess(processName);
                        return contract.getPrice();
                    }
                });
    }

    private Integer getNumberOfPages(List<AttachmentInfo> attachments, boolean isReversePrinter, boolean ignoreAAR){
        if (attachments == null || attachments.isEmpty()) return 0;
        return attachments.stream().map(attachment -> {
            int numberOfPages = attachment.getNumberOfPage();
            if (isReversePrinter) numberOfPages = (int) Math.ceil(((double) attachment.getNumberOfPage())/2);
            return (!ignoreAAR && StringUtils.equals(attachment.getDocumentType(), Const.PN_AAR)) ? numberOfPages-1 : numberOfPages;
        }).reduce(0, Integer::sum);
    }

    private Address saveAddresses(SendRequest sendRequest) {
        String processName = "Save Address";
        log.logStartingProcess(processName);
        Address address = AddressMapper.fromAnalogToAddress(sendRequest.getReceiverAddress(), sendRequest.getProductType().getValue(), Const.EXECUTION);
        PnAddress addressEntity = AddressMapper.toEntity(address,sendRequest.getRequestId(), pnPaperChannelConfig);
        //save receiver address
        log.debug("Inserting data {} with requestId {} in DynamoDb table {}", "addressEntity", sendRequest.getRequestId(), "AddressDynamoTable");
        addressDAO.create(addressEntity);
        log.info("Inserted data in DynamoDb table {}", "AddressDynamoTable");
        if (sendRequest.getSenderAddress() != null) {
            Address senderAddress = AddressMapper.fromAnalogToAddress(sendRequest.getSenderAddress(), sendRequest.getProductType().getValue(),Const.EXECUTION);
            log.debug("Inserting data {} with requestId {} in DynamoDb table {}", "addressEntity", sendRequest.getRequestId(), "AddressDynamoTable");
            addressDAO.create(AddressMapper.toEntity(senderAddress,sendRequest.getRequestId(), AddressTypeEnum.SENDER_ADDRES,pnPaperChannelConfig));
            log.info("Inserted data in DynamoDb table {}", "AddressDynamoTable");
        }
        if (sendRequest.getArAddress() != null) {
            Address arAddress = AddressMapper.fromAnalogToAddress(sendRequest.getArAddress(), sendRequest.getProductType().getValue(),Const.EXECUTION);
            log.debug("Inserting data {} with requestId {} in DynamoDb table {}", "addressEntity", sendRequest.getRequestId(), "AddressDynamoTable");
            addressDAO.create(AddressMapper.toEntity(arAddress,sendRequest.getRequestId(), AddressTypeEnum.AR_ADDRESS, pnPaperChannelConfig));
            log.info("Inserted data in DynamoDb table {}", "AddressDynamoTable");
        }
        log.logEndingProcess(processName);
        return address;
    }


    private Mono<PnDeliveryRequest> saveRequestAndAddress(PrepareRequest prepareRequest){
        String processName = "Save Request and Address";
        log.logStartingProcess(processName);
        PnDeliveryRequest pnDeliveryRequest = RequestDeliveryMapper.toEntity(prepareRequest);
        PnAddress receiverAddressEntity = null;
        PnAddress discoveredAddressEntity = null;

       if (prepareRequest.getReceiverAddress() != null) {
           Address mapped = AddressMapper.fromAnalogToAddress(prepareRequest.getReceiverAddress(), null, Const.PREPARE);
           pnDeliveryRequest.setAddressHash(mapped.convertToHash());
           receiverAddressEntity = AddressMapper.toEntity(mapped, prepareRequest.getRequestId(), pnPaperChannelConfig);
           pnDeliveryRequest.setProductType(getProposalProductType(mapped, pnDeliveryRequest.getProposalProductType()));
           log.info("RequestId - {}, Proposal product type - {}, Product type - {}",
                   pnDeliveryRequest.getRequestId(), pnDeliveryRequest.getProposalProductType(), pnDeliveryRequest.getProductType());
       }

       if (prepareRequest.getDiscoveredAddress() != null) {
           Address mapped = AddressMapper.fromAnalogToAddress(prepareRequest.getDiscoveredAddress(), null, Const.PREPARE);
           pnDeliveryRequest.setHashOldAddress(mapped.convertToHash());
           discoveredAddressEntity = AddressMapper.toEntity(mapped, prepareRequest.getRequestId(), AddressTypeEnum.DISCOVERED_ADDRESS, pnPaperChannelConfig);
       }
        log.logEndingProcess(processName);
        return requestDeliveryDAO.createWithAddress(pnDeliveryRequest, receiverAddressEntity, discoveredAddressEntity);
    }
}
