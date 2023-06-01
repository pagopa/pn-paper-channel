package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.PrepareEventMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.TAKING_CHARGE;

@Service
@CustomLog
public class PrepareAsyncServiceImpl extends BaseService implements PaperAsyncService {

    @Autowired
    private SafeStorageClient safeStorageClient;
    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private PnPaperChannelConfig paperChannelConfig;
    @Autowired
    private PaperRequestErrorDAO paperRequestErrorDAO;
    @Autowired
    private AddressManagerClient addressManagerClient;

    public PrepareAsyncServiceImpl(PnAuditLogBuilder auditLogBuilder, NationalRegistryClient nationalRegistryClient,
                                   RequestDeliveryDAO requestDeliveryDAO,SqsSender sqsQueueSender, CostDAO costDAO ) {
        super(auditLogBuilder, requestDeliveryDAO, costDAO, nationalRegistryClient, sqsQueueSender);
    }

    @Override
    public Mono<PnDeliveryRequest> prepareAsync(PrepareAsyncRequest request){
        String PROCESS_NAME = "Prepare Async";
        log.logStartingProcess(PROCESS_NAME);

        String correlationId = request.getCorrelationId();
        final String requestId = request.getRequestId();
        Address addressFromNationalRegistry = request.getAddress();



        Mono<PnDeliveryRequest> requestDeliveryEntityMono = null;
        if(correlationId!= null) {
            log.info("Start async for {} correlation id", request.getCorrelationId());
            requestDeliveryEntityMono = requestDeliveryDAO.getByCorrelationId(correlationId);
        }else {
            log.info("Start async for {} request id", request.getRequestId());
            requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
        }
        return requestDeliveryEntityMono
                .flatMap(deliveryRequest -> checkAndUpdateAddress(correlationId, deliveryRequest, addressFromNationalRegistry))
                .map(pnDeliveryRequest -> {
                    RequestDeliveryMapper.changeState(
                            pnDeliveryRequest,
                            TAKING_CHARGE.getCode(),
                            TAKING_CHARGE.getDescription(),
                            TAKING_CHARGE.getDetail(),
                            pnDeliveryRequest.getProductType(),
                            null
                    );
                    return pnDeliveryRequest;
                })
                .flatMap(pnDeliveryRequest -> getAttachmentsInfo(pnDeliveryRequest, request))
                .flatMap(pnDeliveryRequest ->
                     this.addressDAO.findByRequestId(pnDeliveryRequest.getRequestId())
                            .flatMap(address -> {
                                this.sqsSender.pushPrepareEvent(PrepareEventMapper.toPrepareEvent(pnDeliveryRequest, AddressMapper.toDTO(address), StatusCodeEnum.OK));
                                return this.requestDeliveryDAO.updateData(pnDeliveryRequest);
                            })
                )
                .onErrorResume(ex -> {
                    log.error("Error prepare async requestId {}, {}", requestId, ex.getMessage(), ex);
                    StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;
                    if(ex instanceof PnGenericException) {
                        statusDeliveryEnum = mapper(((PnGenericException) ex).getExceptionType());
                    }
                    return updateStatus(requestId, correlationId, statusDeliveryEnum)
                            .doOnNext(entity -> {
                                if (entity.getStatusCode().equals(StatusDeliveryEnum.UNTRACEABLE.getCode())){
                                    sendUnreachableEvent(entity);
                                    log.logEndingProcess(PROCESS_NAME);
                                }
                            })
                            .flatMap(entity -> Mono.error(ex));
                });
    }

    private StatusDeliveryEnum mapper(ExceptionTypeEnum ex){
        return switch (ex) {
            case UNTRACEABLE_ADDRESS -> StatusDeliveryEnum.UNTRACEABLE;
            case DOCUMENT_NOT_DOWNLOADED -> StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
            case DOCUMENT_URL_NOT_FOUND -> StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
            default -> StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
        };

    }

    public Mono<PnDeliveryRequest> updateStatus(String requestId, String correlationId, StatusDeliveryEnum status ){
        String processName = "Update Status";
        log.logStartingProcess(processName);
        Mono<PnDeliveryRequest> pnDeliveryRequest;
        if (StringUtils.isNotEmpty(requestId) && !StringUtils.isNotEmpty(correlationId) ){
            pnDeliveryRequest= this.requestDeliveryDAO.getByRequestId(requestId);
        }else{
            pnDeliveryRequest= this.requestDeliveryDAO.getByCorrelationId(correlationId);
        }
        log.logEndingProcess(processName);
        return pnDeliveryRequest.flatMap(
                entity -> {
                    RequestDeliveryMapper.changeState(
                            entity,
                            status.getCode(),
                            status.getDescription(),
                            status.getDetail(),
                            null,
                            null
                    );
                    return this.requestDeliveryDAO.updateData(entity);
                });
    }

    private Mono<PnDeliveryRequest> checkAndUpdateAddress(String correlationId, PnDeliveryRequest pnDeliveryRequest, Address fromNationalRegistries){
        String VALIDATION_NAME = "Check and update address";
        log.logChecking(VALIDATION_NAME);
        pnLogAudit.addsBeforeResolveLogic(
                pnDeliveryRequest.getIun(),
                String.format("prepare requestId = %s, relatedRequestId = %s Is National Registry Address present ?",
                        pnDeliveryRequest.getRequestId(),
                        pnDeliveryRequest.getRelatedRequestId())
        );
        if (StringUtils.isNotBlank(correlationId)) {

            pnLogAudit.addsSuccessResolveLogic(
                    pnDeliveryRequest.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s National Registry Address is present",
                            pnDeliveryRequest.getRequestId(),
                            pnDeliveryRequest.getRelatedRequestId())
            );

            if (fromNationalRegistries == null){
                pnLogAudit.addsSuccessResolveLogic(
                        pnDeliveryRequest.getIun(),
                        String.format("prepare requestId = %s, relatedRequestId = %s National Registry Address is null",
                                pnDeliveryRequest.getRequestId(),
                                pnDeliveryRequest.getRelatedRequestId())
                );
                log.logCheckingOutcome(VALIDATION_NAME, false, UNTRACEABLE_ADDRESS.getMessage());
                return Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage()));
            }

            return this.addressDAO.findByRequestId(pnDeliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS)
                    .switchIfEmpty(Mono.defer(() -> {
                        log.logCheckingOutcome(VALIDATION_NAME, false, ADDRESS_NOT_EXIST.getMessage());
                        log.error("Receiver Address for {} request id not found on DB", pnDeliveryRequest.getRequestId());
                        throw new PnGenericException(ADDRESS_NOT_EXIST, ADDRESS_NOT_EXIST.getMessage());
                    }))
                    .map(AddressMapper::toDTO)
                    .doOnNext(address ->
                        pnLogAudit.addsBeforeResolveLogic(
                                pnDeliveryRequest.getIun(),
                                String.format("prepare requestId = %s, relatedRequestId = %s Is National Registry Address is not equals previous address ?",
                                        pnDeliveryRequest.getRequestId(),
                                        pnDeliveryRequest.getRelatedRequestId())
                        )
                    )
                    .zipWhen(receiverAddress -> addressManagerClient.deduplicates(correlationId, receiverAddress, fromNationalRegistries))
                    .flatMap(receiverAddressAndResponseDeduplicates -> {
                       if (Boolean.TRUE.equals(receiverAddressAndResponseDeduplicates.getT2().getEqualityResult())) {
                           pnLogAudit.addsSuccessResolveLogic(
                                   pnDeliveryRequest.getIun(),
                                   String.format("prepare requestId = %s, relatedRequestId = %s National Registry Address is equals previous address",
                                           pnDeliveryRequest.getRequestId(),
                                           pnDeliveryRequest.getRelatedRequestId())
                           );
                           log.logCheckingOutcome(VALIDATION_NAME, false, UNTRACEABLE_ADDRESS.getMessage());
                           return Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage()));
                       }
                       if (receiverAddressAndResponseDeduplicates.getT2().getError() != null){
                           log.logCheckingOutcome(VALIDATION_NAME, false, receiverAddressAndResponseDeduplicates.getT2().getError());
                           log.error("Response from address manager {} with request id {}", receiverAddressAndResponseDeduplicates.getT2().getError(), pnDeliveryRequest.getRequestId());
                           return Mono.error(new PnGenericException(ADDRESS_MANAGER_ERROR, receiverAddressAndResponseDeduplicates.getT2().getError()));
                       }
                        pnLogAudit.addsSuccessResolveLogic(
                                pnDeliveryRequest.getIun(),
                                String.format("prepare requestId = %s, relatedRequestId = %s National Registry Address is not equals previous address",
                                        pnDeliveryRequest.getRequestId(),
                                        pnDeliveryRequest.getRelatedRequestId())
                        );
                        AnalogAddressDto addressFromManager = receiverAddressAndResponseDeduplicates.getT2().getNormalizedAddress();

                        if (addressFromManager == null) {
                            log.error("Response from address manager have a address null {}", pnDeliveryRequest.getRequestId());
                            log.logCheckingOutcome(VALIDATION_NAME, false, UNTRACEABLE_ADDRESS.getMessage());
                            return Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage()));
                        }
                        Address address = AddressMapper.fromAnalogAddressManager(addressFromManager) ;
                        address.setFullName(receiverAddressAndResponseDeduplicates.getT1().getFullName());
                        address.setNameRow2(receiverAddressAndResponseDeduplicates.getT1().getNameRow2());
                        return Mono.just(address);
                    })
                    .flatMap(newAddress -> {
                        log.logCheckingOutcome(VALIDATION_NAME, true);
                        pnDeliveryRequest.setAddressHash(newAddress.convertToHash());
                        pnDeliveryRequest.setProductType(getProposalProductType(newAddress, pnDeliveryRequest.getProposalProductType()));

                        //set flowType per TTL
                        newAddress.setFlowType(Const.PREPARE);
                        return addressDAO.create(AddressMapper.toEntity(newAddress, pnDeliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, paperChannelConfig))
                                .map(item -> pnDeliveryRequest);
                    })
                    .onErrorResume(ex ->
                        traceError(pnDeliveryRequest.getRequestId(), ex.getMessage(), "CHECK_ADDRESS_FLOW" )
                            .then(Mono.defer(() -> Mono.error(ex)))
                    );

        }

        pnLogAudit.addsSuccessResolveLogic(
                pnDeliveryRequest.getIun(),
                String.format("prepare requestId = %s, relatedRequestId = %s Is National Registry Address is not present",
                        pnDeliveryRequest.getRequestId(),
                        pnDeliveryRequest.getRelatedRequestId())
        );

        if (StringUtils.isNotBlank(pnDeliveryRequest.getRelatedRequestId())) {
            pnLogAudit.addsBeforeResolveLogic(
                    pnDeliveryRequest.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s Is Discovered Address present ?",
                            pnDeliveryRequest.getRequestId(),
                            pnDeliveryRequest.getRelatedRequestId())
            );
            return addressDAO.findByRequestId(pnDeliveryRequest.getRequestId(), AddressTypeEnum.DISCOVERED_ADDRESS)
                    .doOnNext(discovered ->
                            pnLogAudit.addsSuccessResolveLogic(
                                    pnDeliveryRequest.getIun(),
                                    String.format("prepare requestId = %s, relatedRequestId = %s Discovered Address is present",
                                            pnDeliveryRequest.getRequestId(),
                                            pnDeliveryRequest.getRelatedRequestId())
                            )
                    )
                    .map(AddressMapper::toDTO)
                    .flatMap(newAddress -> {
                        pnDeliveryRequest.setAddressHash(newAddress.convertToHash());
                        pnDeliveryRequest.setProductType(getProposalProductType(newAddress, pnDeliveryRequest.getProposalProductType()));

                        newAddress.setFlowType(Const.PREPARE);
                        return addressDAO.create(AddressMapper.toEntity(newAddress, pnDeliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, paperChannelConfig))
                                .map(item -> pnDeliveryRequest);
                    });
        }

        pnLogAudit.addsResolveLogic(
                pnDeliveryRequest.getIun(),
                String.format("prepare requestId = %s Is receiver address present ?", pnDeliveryRequest.getRequestId()),
                String.format("prepare requestId = %s receiver address is present", pnDeliveryRequest.getRequestId()));
        return Mono.just(pnDeliveryRequest);
    }

    public Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis){
        if (n<0)
            return Mono.error(new PnGenericException( DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage() ) );
        else {
            return Mono.delay(Duration.ofMillis( millis.longValue() ))
                     .flatMap(item -> safeStorageClient.getFile(fileKey)
                     .map(fileDownloadResponseDto -> fileDownloadResponseDto)
                     .onErrorResume(ex -> {
                         log.error ("Error with retrieve {}", ex.getMessage());
                         return Mono.error(ex);
                     })
                     .onErrorResume(PnRetryStorageException.class, ex ->
                         getFileRecursive(n - 1, fileKey, ex.getRetryAfter())
                    ));
        }
    }

    private Mono<PnDeliveryRequest> getAttachmentsInfo(PnDeliveryRequest deliveryRequest, PrepareAsyncRequest request){

        if(deliveryRequest.getAttachments().isEmpty() ||
                !deliveryRequest.getAttachments().stream().filter(a ->a.getNumberOfPage()!=null && a.getNumberOfPage()>0).toList().isEmpty()){
            return Mono.just(deliveryRequest);
        }

        return Flux.fromStream(deliveryRequest.getAttachments().stream())
                .parallel()
                .flatMap( attachment -> getFileRecursive(
                        paperChannelConfig.getAttemptSafeStorage(),
                        attachment.getFileKey(),
                        new BigDecimal(0))
                )
                .flatMap(fileResponse -> {
                    AttachmentInfo info = AttachmentMapper.fromSafeStorage(fileResponse);
                    if (info.getUrl() == null)
                        return Flux.error(new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage()));
                    return HttpConnector.downloadFile(info.getUrl())
                            .map(pdDocument -> {
                                try {
                                    if (pdDocument.getDocumentInformation() != null && pdDocument.getDocumentInformation().getCreationDate() != null) {
                                        info.setDate(DateUtils.formatDate(pdDocument.getDocumentInformation().getCreationDate().getTime()));
                                    }
                                    info.setNumberOfPage(pdDocument.getNumberOfPages());
                                    pdDocument.close();
                                } catch (IOException e) {
                                    throw new PnGenericException(INVALID_SAFE_STORAGE, INVALID_SAFE_STORAGE.getMessage());
                                }
                                return info;
                            });

                })
                .map(AttachmentMapper::toEntity)
                .sequential()
                .collectList()
                .map(listAttachment -> {
                    deliveryRequest.setAttachments(listAttachment);
                    return deliveryRequest;
                })
                .onErrorResume(ex -> {
                    request.setIun(deliveryRequest.getIun());
                    this.sqsSender.pushInternalError(request, request.getAttemptRetry(), PrepareAsyncRequest.class);
                    return Mono.error(ex);
                });
    }


    private void sendUnreachableEvent(PnDeliveryRequest request){
        log.debug("Send Unreachable Event request id - {}, iun - {}", request.getRequestId(), request.getIun());
        this.sqsSender.pushPrepareEvent(PrepareEventMapper.toPrepareEvent(request, null, StatusCodeEnum.KOUNREACHABLE));
    }

    private Mono<Void> traceError(String requestId, String error, String flowType){
        return this.paperRequestErrorDAO.created(requestId, error, flowType)
                .then();
    }

}
