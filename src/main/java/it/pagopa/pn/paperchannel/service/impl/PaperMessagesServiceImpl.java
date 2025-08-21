package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.mapper.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.PaperTrackerClient;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.*;
import it.pagopa.pn.paperchannel.utils.costutils.CostWithDriver;
import it.pagopa.pn.paperchannel.validator.PrepareRequestValidator;
import it.pagopa.pn.paperchannel.validator.SendRequestValidator;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_IN_PROCESSING;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.READY_TO_SEND;
import static it.pagopa.pn.paperchannel.utils.Const.CONTEXT_KEY_CLIENT_ID;
import static it.pagopa.pn.paperchannel.utils.Const.CONTEXT_KEY_PREFIX_CLIENT_ID;

@CustomLog
@Service
public class PaperMessagesServiceImpl extends GenericService implements PaperMessagesService {

    private static final String PN_DELIVERY_REQUEST_LOG = "PnDeliveryRequest";
    private static final String ADDRESS_ENTITY_LOG = "addressEntity";
    private static final String INSERTED_IN_DYNAMO_LOG = "Inserted data in DynamoDb table {}";
    private static final String INSERTING_IN_DYNAMO_LOG = "Inserting data {} with requestId {} in DynamoDb table {}";

    private final AddressDAO addressDAO;
    private final PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;
    private final ExternalChannelClient externalChannelClient;
    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final PaperCalculatorUtils paperCalculatorUtils;
    private final PrepareFlowStarter prepareFlowStarter;
    private final NationalRegistryService nationalRegistryService;
    private final PaperTrackerClient paperTrackerClient;


    public PaperMessagesServiceImpl(RequestDeliveryDAO requestDeliveryDAO, SqsSender sqsSender, AddressDAO addressDAO, PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO,
                                    ExternalChannelClient externalChannelClient, PnPaperChannelConfig pnPaperChannelConfig,
                                    PaperCalculatorUtils paperCalculatorUtils, PrepareFlowStarter prepareFlowStarter, NationalRegistryService nationalRegistryService, PaperTrackerClient paperTrackerClient) {
        super(sqsSender, requestDeliveryDAO);
        this.addressDAO = addressDAO;
        this.paperChannelDeliveryDriverDAO = paperChannelDeliveryDriverDAO;
        this.externalChannelClient = externalChannelClient;
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.paperCalculatorUtils = paperCalculatorUtils;
        this.prepareFlowStarter = prepareFlowStarter;
        this.nationalRegistryService = nationalRegistryService;
        this.paperTrackerClient = paperTrackerClient;
    }

    @Override
    public Mono<PaperChannelUpdate> preparePaperSync(String requestId, PrepareRequest prepareRequest){

        PnLogAudit pnLogAudit = new PnLogAudit();

        prepareRequest.setRequestId(requestId);
        if (StringUtils.isEmpty(prepareRequest.getRelatedRequestId())){
            log.info("First attempt requestId {}", requestId);

            //case of 204
            log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table  RequestDeliveryDynamoTable", requestId);
            return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                    .doOnNext(entity -> log.info("Founded data in DynamoDB  table RequestDeliveryDynamoTable"))
                    .doOnNext(entity -> PrepareRequestValidator.compareRequestEntity(prepareRequest, entity, true, false))
                    .doOnNext(entity -> log.debug("Getting PnAddress with requestId {}, in  DynamoDB table AddressDynamoTable", requestId))
                    .flatMap(entity -> checkIfReworkNeededAndReturnPaperChannelUpdate(prepareRequest, entity))
                    .switchIfEmpty(
                            Mono.defer(() -> saveRequestAndAddress(prepareRequest, null)
                                                        .flatMap(this::createAndPushPrepareEvent)
                                                        .then(Mono.empty()))
                    );
        }

        log.info("Second attempt requestId {}", requestId);
        log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table RequestDeliveryDynamoTable", requestId);
        return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRelatedRequestId())
                .doOnNext(oldEntity -> {
                    log.info("Founded data in DynamoDB table RequestDeliveryDynamoTable");
                    prepareRequest.setRequestId(oldEntity.getRequestId());
                    PrepareRequestValidator.compareRequestEntity(prepareRequest, oldEntity, false, true);
                })
                .zipWhen(oldEntity -> {
                    log.debug("Getting PnAddress with requestId {}, in DynamoDB table AddressDynamoTable", oldEntity.getRequestId());
                    return addressDAO.findByRequestId(oldEntity.getRequestId())
                            .doOnNext(address -> log.info("Founded data in DynamoDB table AddressDynamoTable"))
                            .doOnNext(address -> log.debug("Address of the related request"))
                            .doOnNext(address -> log.debug(
                                    "name surname: {}, address: {}, zip: {},  foreign state: {}",
                                    LogUtils.maskGeneric(address.getFullName()),
                                    LogUtils.maskGeneric(address.getAddress()),
                                    LogUtils.maskGeneric(address.getCap()),
                                    LogUtils.maskGeneric(address.getCountry())
                            ));
                }, (entity, address) -> entity)
                .flatMap(oldEntity -> {
                    prepareRequest.setRequestId(requestId);
                    log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table RequestDeliveryDynamoTable", requestId);
                    return this.requestDeliveryDAO.getByRequestId(prepareRequest.getRequestId())
                            .doOnNext(secondDeliveryRequest -> log.info("Attempt already exist for request id : {}", secondDeliveryRequest.getRequestId()))
                            .doOnNext(secondDeliveryRequest -> log.info("Founded data in DynamoDB table RequestDeliveryDynamoTable"))
                            .doOnNext(secondDeliveryRequest -> PrepareRequestValidator.compareRequestEntity(prepareRequest, secondDeliveryRequest, false, false))
                            .flatMap(secondDeliveryRequest -> checkIfReworkNeededAndReturnPaperChannelUpdate(prepareRequest, secondDeliveryRequest))
                            .switchIfEmpty(Mono.deferContextual(cxt ->
                                    saveRequestAndAddress(prepareRequest, oldEntity.getApplyRasterization())
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

                                                    final String clientId = cxt.getOrDefault(CONTEXT_KEY_CLIENT_ID, "");
                                                    prepareFlowStarter.startPreparePhaseOneFromPrepareSync(response, clientId);
                                                } else {
                                                    pnLogAudit.addsSuccessResolveLogic(
                                                            prepareRequest.getIun(),
                                                            String.format("prepare requestId = %s, relatedRequestId = %s Discovered Address is not present", requestId, prepareRequest.getRelatedRequestId())
                                                    );
                                                    nationalRegistryService.finderAddressFromNationalRegistries(
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
        log.debug("Getting PnDeliveryRequest with requestId {}, in DynamoDB table {}", requestId, PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME);
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> {
                            log.info("Founded data in DynamoDB table {}", PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME);
                            log.debug("Getting PnAddress with requestId {}, in DynamoDB table {}", requestId, PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
                            return addressDAO.findByRequestId(requestId).map(address -> {
                                        log.info("Founded data in DynamoDB table {}", PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
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

        PnLogAudit pnLogAudit = new PnLogAudit();

        log.info("Start executionPaper with requestId {}", requestId);
        sendRequest.setRequestId(requestId);

        log.debug("Getting  data {} with requestId {} in DynamoDb table {}", PN_DELIVERY_REQUEST_LOG, requestId, PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME);
        return this.requestDeliveryDAO.getByRequestId(sendRequest.getRequestId())
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)))
                .map(entity -> {
                    log.info("Founded data in  DynamoDb table {}", PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME);
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
                    final String VALIDATION_NAME = "Amount calculation process";
                    log.logChecking(VALIDATION_NAME);

                    final boolean isReversePrinter = StringUtils.equals(sendRequest.getPrintType(), Const.BN_FRONTE_RETRO);

                    return paperCalculatorUtils.calculator(attachments, address, sendRequest.getProductType(), isReversePrinter)
                        .map(costWithDriver -> {
                            pnDeliveryRequest.setDriverCode(costWithDriver.getDriverCode());
                            pnDeliveryRequest.setTenderCode(costWithDriver.getTenderCode());

                            return getSendResponse(costWithDriver, attachments, isReversePrinter);
                        })
                        .map(sendResponse -> {
                            // controllo se il costo (eventuale) usato nella prepare è uguale a quello attuale nella send.
                            // se così non dovesse essere, viene lanciata una exception
                            SendRequestValidator.compareRequestCostEntity(sendResponse.getAmount(), pnDeliveryRequest.getCost());

                            log.logCheckingOutcome(VALIDATION_NAME, true);
                            return Tuples.of(sendResponse, pnDeliveryRequest, attachments, address);
                        })
                        .doOnError(ex -> log.logCheckingOutcome(VALIDATION_NAME, false, ex.getMessage()))
                        .onErrorResume(PnGenericException.class, pnGenericException -> {
                            // mi segno che la richiesta richiede una nuova prepare, impostando il flag di "reworkNeeded" a TRUE
                            if (pnGenericException.getExceptionType() == ExceptionTypeEnum.DIFFERENT_SEND_COST) {
                                pnDeliveryRequest.setReworkNeeded(true);
                                return requestDeliveryDAO.updateData(pnDeliveryRequest)
                                    .flatMap(x -> Mono.error(pnGenericException));
                            }
                            else
                                return Mono.error(pnGenericException);
                        });
                })
                .flatMap(tuple -> {
                    SendResponse sendResponse = tuple.getT1();
                    PnDeliveryRequest pnDeliveryRequest = tuple.getT2();
                    List<AttachmentInfo> attachments = tuple.getT3();

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

                        if (pnPaperChannelConfig.isPaperTrackerEnabled() && pnPaperChannelConfig.getPaperTrackerProductList().contains(pnDeliveryRequest.getProductType())) {
                            return paperChannelDeliveryDriverDAO.getByDeliveryDriverId(pnDeliveryRequest.getDriverCode())
                                    .map(PaperChannelDeliveryDriver::getUnifiedDeliveryDriver)
                                    .flatMap(unifiedDeliveryDriver -> paperTrackerClient.initPaperTracking(Utility.getRequestIdWithParams(requestId, "0", null), pnDeliveryRequest.getProductType(), unifiedDeliveryDriver))
                                    .onErrorResume(PnIdConflictException.class, ex -> Mono.just(pnDeliveryRequest))
                                    .flatMap(unused -> sendEngage(requestId, sendResponse, sendRequest, pnDeliveryRequest, attachments, pnLogAudit));
                        } else {
                            return sendEngage(requestId, sendResponse, sendRequest, pnDeliveryRequest, attachments, pnLogAudit);
                        }
                    }
                    return Mono.just(sendResponse);
                });
    }

    private Mono<SendResponse> sendEngage(String requestId, SendResponse sendResponse, SendRequest sendRequest, PnDeliveryRequest pnDeliveryRequest,  List<AttachmentInfo> attachments, PnLogAudit pnLogAudit) {
        return sendEngageExternalChannel(sendRequest, attachments, pnDeliveryRequest.getApplyRasterization())
                .then(Mono.defer(() -> {
                    log.debug("Updating data {} with requestId {} in DynamoDb table {}", PN_DELIVERY_REQUEST_LOG, requestId, PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME);
                    return this.requestDeliveryDAO.updateData(pnDeliveryRequest);
                }))
                .doOnNext(requestUpdated -> log.info("Updated data in DynamoDb table {}", PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME))
                .map(requestUpdated -> sendResponse)
                .doOnError(ex -> {
                    String logString = "prepare requestId = %s, trace_id = %s  request to  External Channel";
                    logString = String.format(logString, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
                    pnLogAudit.addsFailSend(sendRequest.getIun(), logString);
                });
    }


    private Mono<Void> sendEngageExternalChannel(SendRequest sendRequest, List<AttachmentInfo> attachments, Boolean applyRasterization){

        PnLogAudit pnLogAudit = new PnLogAudit();

        return Utility.getFromContext(CONTEXT_KEY_PREFIX_CLIENT_ID, "")
                .switchIfEmpty(Mono.just(""))
                .map(clientIdPrefix -> Utility.getRequestIdWithParams(sendRequest.getRequestId(), "0", clientIdPrefix))
                .map(sendRequest::requestId)
                .doOnNext(newSendRequest -> {
                    String logString = "prepare requestId = %s, trace_id = %s  request to External Channel";
                    logString = String.format(logString, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
                    pnLogAudit.addsBeforeSend(sendRequest.getIun(), logString);
                })
                .flatMap(newSendRequest -> this.externalChannelClient.sendEngageRequest(newSendRequest, attachments, applyRasterization))
                .doOnSuccess(response -> {
                    String logString = "prepare requestId = %s, trace_id = %s  request to External Channel";
                    logString = String.format(logString, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
                    pnLogAudit.addsSuccessSend(sendRequest.getIun(),logString);
                });
    }

    @Override
    public Mono<SendEvent> retrievePaperSendRequest(String requestId) {
        log.debug("Getting data {} with requestId {} in DynamoDb table {}", PN_DELIVERY_REQUEST_LOG, requestId, PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME);
        return requestDeliveryDAO.getByRequestId(requestId)
                .zipWhen(entity -> {
                    log.info("Founded data in DynamoDb table {}", PnDeliveryRequest.REQUEST_DELIVERY_DYNAMO_TABLE_NAME);

                    if (entity.getStatusCode().equals(StatusDeliveryEnum.TAKING_CHARGE.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.IN_PROCESSING.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR.getCode())
                            || entity.getStatusCode().equals(StatusDeliveryEnum.PAPER_CHANNEL_NEW_REQUEST.getCode())) {
                        return Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND));
                    }
                    log.debug("Getting data {} with requestId {} in DynamoDb table {}", "PnAddress", requestId, PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
                    return addressDAO.findByRequestId(requestId).map(address -> {
                                log.info("Founded data in DynamoDb table {}", PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
                                return address;
                            })
                            .switchIfEmpty(Mono.just(new PnAddress()));
                })
                .map(entityAndAddress -> SendEventMapper.fromResult(entityAndAddress.getT1(),entityAndAddress.getT2()))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
    }

    private SendResponse getSendResponse(CostWithDriver costWithDriver, List<AttachmentInfo> attachments, boolean isReversePrinter){
        Integer totalPages = this.paperCalculatorUtils.getNumberOfPages(attachments, isReversePrinter, true);
        Integer amountPriceFormat = Utility.toCentsFormat(costWithDriver.getCost());

        log.debug("Amount : {}", costWithDriver.getCost());
        log.debug("Total pages : {}", totalPages);

        SendResponse response = new SendResponse();
        response.setAmount(amountPriceFormat);
        response.setNumberOfPages(totalPages);
        response.setEnvelopeWeight(this.paperCalculatorUtils.getLetterWeight(totalPages, pnPaperChannelConfig.getPaperWeight(), pnPaperChannelConfig.getLetterWeight()));

        return response;
    }

    /**
     * controllo se la richiesta ha il flag di reworkNeeded a true, che ricordo essere così se per qualche motivo (es: aggiornamento costo gare)
     * c'è un mismatch tra il costo calcolato nella PREPARE e usato nella generazione degli F24 e il costo calcolato nella SEND.
     *
     * Se si RISALVA l'entity di deliveryrequest perchè voglio RIESEGUIRE la logica di calcolo del costo e generazione degli F24.
     * Da notare che questo metodo vien invocato anche nel caso della seconda raccomandata. In questo caso,
     * se il flag reworkNeeded è true, in realtà alla "prima invocazione della prepare della seconda raccomandata" ho
     * già fatto tutta la logica di risoluzione dell'indirizzo, e quindi non serve rieseguirla, motivo per cui metto in coda
     * direttamente l'evento di "prepareAsync" (che calcolerà nuovamente il costo e rigenererà i pdf F24 aggiornati).
     *
     * @param prepareRequest la richiesta di prepare
     * @param pnDeliveryRequest l'entity precedentemente salvata in db
     * @return la risposta da tornare al chiamante
     */
    private Mono<PaperChannelUpdate> checkIfReworkNeededAndReturnPaperChannelUpdate(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryRequest){
        if (Boolean.TRUE.equals(pnDeliveryRequest.getReworkNeeded()))
        {
            log.info("Call PREPARE Sync with rework-needed=true");
            return saveRequestAndAddress(prepareRequest, true, pnDeliveryRequest.getReworkNeededCount(), pnDeliveryRequest.getApplyRasterization())
                    .flatMap(this::createAndPushPrepareEvent)
                    .then(Mono.just(PreparePaperResponseMapper.fromResult(pnDeliveryRequest, null)));
        }
        else {
            log.debug("Getting PnAddress with requestId {}, in DynamoDB table AddressDynamoTable", prepareRequest.getRequestId());
            return addressDAO.findByRequestId(prepareRequest.getRequestId())
                    .doOnNext(address -> log.info("Founded data  in DynamoDB table AddressDynamoTable"))
                    .map(address -> PreparePaperResponseMapper.fromResult(pnDeliveryRequest, address))
                    .switchIfEmpty(Mono.just(PreparePaperResponseMapper.fromResult(pnDeliveryRequest, null)));
        }
    }

    private Address saveAddresses(SendRequest sendRequest) {
        String processName = "Save Address";
        log.logStartingProcess(processName);
        Address address = AddressMapper.fromAnalogToAddress(sendRequest.getReceiverAddress(), sendRequest.getProductType().getValue(), Const.EXECUTION);
        PnAddress addressEntity = AddressMapper.toEntity(address,sendRequest.getRequestId(), pnPaperChannelConfig);
        //save receiver address
        log.debug(INSERTING_IN_DYNAMO_LOG, ADDRESS_ENTITY_LOG, sendRequest.getRequestId(), PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
        addressDAO.create(addressEntity);
        log.info(INSERTED_IN_DYNAMO_LOG, PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
        if (sendRequest.getSenderAddress() != null) {
            Address senderAddress = AddressMapper.fromAnalogToAddress(sendRequest.getSenderAddress(), sendRequest.getProductType().getValue(),Const.EXECUTION);
            log.debug(INSERTING_IN_DYNAMO_LOG, ADDRESS_ENTITY_LOG, sendRequest.getRequestId(), PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
            addressDAO.create(AddressMapper.toEntity(senderAddress,sendRequest.getRequestId(), AddressTypeEnum.SENDER_ADDRES,pnPaperChannelConfig));
            log.info(INSERTED_IN_DYNAMO_LOG, PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
        }
        if (sendRequest.getArAddress() != null) {
            Address arAddress = AddressMapper.fromAnalogToAddress(sendRequest.getArAddress(), sendRequest.getProductType().getValue(),Const.EXECUTION);
            log.debug(INSERTING_IN_DYNAMO_LOG, ADDRESS_ENTITY_LOG, sendRequest.getRequestId(), PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
            addressDAO.create(AddressMapper.toEntity(arAddress,sendRequest.getRequestId(), AddressTypeEnum.AR_ADDRESS, pnPaperChannelConfig));
            log.info(INSERTED_IN_DYNAMO_LOG, PnAddress.ADDRESS_DYNAMO_TABLE_NAME);
        }
        log.logEndingProcess(processName);
        return address;
    }


    private Mono<PnDeliveryRequest> saveRequestAndAddress(PrepareRequest prepareRequest, boolean reworkNeeded, Integer reworkNeededCount, Boolean applyRasterization){
        String processName = "Save Request and Address";
        log.logStartingProcess(processName);

        PnDeliveryRequest pnDeliveryRequest = RequestDeliveryMapper.toEntity(prepareRequest);
        // Init refined field as false
        pnDeliveryRequest.setRefined(false);
        pnDeliveryRequest.setApplyRasterization(applyRasterization);

        PnAddress receiverAddressEntity = null;
        PnAddress discoveredAddressEntity = null;

        if (prepareRequest.getReceiverAddress() != null) {
            Address mapped = AddressMapper.fromAnalogToAddress(prepareRequest.getReceiverAddress(), null, Const.PREPARE);
            pnDeliveryRequest.setAddressHash(mapped.convertToHash());
            receiverAddressEntity = AddressMapper.toEntity(mapped, prepareRequest.getRequestId(), pnPaperChannelConfig);
            pnDeliveryRequest.setProductType(this.paperCalculatorUtils.getProposalProductType(mapped, pnDeliveryRequest.getProposalProductType()));
            log.info("RequestId - {}, Proposal product type - {}, Product type - {}",
                    pnDeliveryRequest.getRequestId(), pnDeliveryRequest.getProposalProductType(), pnDeliveryRequest.getProductType());
        }

        if (prepareRequest.getDiscoveredAddress() != null) {
            Address mapped = AddressMapper.fromAnalogToAddress(prepareRequest.getDiscoveredAddress(), null, Const.PREPARE);
            pnDeliveryRequest.setHashOldAddress(mapped.convertToHash());
            discoveredAddressEntity = AddressMapper.toEntity(mapped, prepareRequest.getRequestId(), AddressTypeEnum.DISCOVERED_ADDRESS, pnPaperChannelConfig);
        }

        if(reworkNeeded) {
            pnDeliveryRequest.setReworkNeeded(true);
            if(pnDeliveryRequest.getReworkNeededCount() == null) {
                pnDeliveryRequest.setReworkNeededCount(1);
            }
            else {
                pnDeliveryRequest.setReworkNeededCount(reworkNeededCount + 1);
            }
        }

        log.logEndingProcess(processName);
        return requestDeliveryDAO.createWithAddress(pnDeliveryRequest, receiverAddressEntity, discoveredAddressEntity);
    }

    private Mono<PnDeliveryRequest> saveRequestAndAddress(PrepareRequest prepareRequest, Boolean applyRasterization){
        return saveRequestAndAddress(prepareRequest, false, 0, applyRasterization);
    }

    private Mono<Void> createAndPushPrepareEvent(PnDeliveryRequest deliveryRequest){
        return Utility.getFromContext(CONTEXT_KEY_CLIENT_ID, "")
                .switchIfEmpty(Mono.just(""))
                .doOnNext(clientId -> prepareFlowStarter.startPreparePhaseOneFromPrepareSync(deliveryRequest, clientId))
                .then();

    }

}