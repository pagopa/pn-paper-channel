package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.PrepareEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.utils.Const.*;

@Slf4j
@Service
public class PrepareAsyncServiceImpl extends BaseService implements PaperAsyncService {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private SafeStorageClient safeStorageClient;
    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private SqsSender sqsQueueSender;

    @Autowired
    private NationalRegistryClient nationalRegistryClient;

    public PrepareAsyncServiceImpl(PnAuditLogBuilder auditLogBuilder) {
        super(auditLogBuilder);
    }

    @Override
    public Mono<PnDeliveryRequest> prepareAsync(PrepareAsyncRequest request){
        log.info("Start async");
        String correlationId = request.getCorrelationId();
        String requestId = request.getRequestId();
        Address addressFromNationalRegistry = request.getAddress() ;
        Mono<PnDeliveryRequest> requestDeliveryEntityMono =null;
        if(correlationId!= null)
            requestDeliveryEntityMono = requestDeliveryDAO.getByCorrelationId(correlationId);
        else
            requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);

        return requestDeliveryEntityMono
                .zipWhen(entity -> addressDAO.findByRequestId(entity.getRequestId()).map(item->item))
                .map(pnDeliveryRequestPnAddressTuple2 -> {
                            if (StringUtils.isNotBlank(correlationId) && request.getIsSecondAttempt() ==true)
                             this.nationalRegistryClient.finderAddress(pnDeliveryRequestPnAddressTuple2.getT1().getFiscalCode(), pnDeliveryRequestPnAddressTuple2.getT1().getReceiverType());
                          return pnDeliveryRequestPnAddressTuple2;
                        }
                )
                .map(entityAndAddress -> {

                    PnDeliveryRequest pnDeliveryRequest = entityAndAddress.getT1();

                    Address correctAddress = AddressMapper.toDTO(entityAndAddress.getT2());
                    if (StringUtils.isNotBlank(correlationId)){
                        /*
                        se siamo nel secondo tentativo dobbiamo fare i controlli su:
                            - indirizzo recuperato da National Registry
                            - hash indirizzo primo tentativo
                            - indirizzo scoperto dal postino se != null
                        */
                        correctAddress = setCorrectAddress(pnDeliveryRequest.getRequestId(), pnDeliveryRequest.getIun(), pnDeliveryRequest.getAddressHash(), addressFromNationalRegistry, correctAddress);
                    }

                    if (pnDeliveryRequest.getProductType() == null ) {
                        setLetterCode(correctAddress, pnDeliveryRequest);
                    }

                    pnDeliveryRequest.setStatusCode(StatusDeliveryEnum.TAKING_CHARGE.getCode());
                    pnDeliveryRequest.setStatusDetail(StatusDeliveryEnum.TAKING_CHARGE.getDescription());
                    pnDeliveryRequest.setStatusDate(DateUtils.formatDate(new Date()));
                    return Tuples.of(pnDeliveryRequest, correctAddress);
                })

                .zipWhen(deliveryRequestAndAddress -> getAttachmentsInfo(deliveryRequestAndAddress.getT1()).map(newModel -> newModel)
                        //Ritorno la tupla composta dall'indirizzo e il risultato ottentuto da attachment info
                        ,(input, output) -> Tuples.of(output, input.getT2())
                )
                .flatMap(deliveryRequestAndAddress -> {
                    //Controllo se l'indirizzo che ho proviene da NationalRegistry
                    if (deliveryRequestAndAddress.getT2().isFromNationalRegistry()){
                        log.info("National registry address");

                        return addressDAO.create(AddressMapper.toEntity(addressFromNationalRegistry, deliveryRequestAndAddress.getT1().getRequestId()))
                                .map(item -> deliveryRequestAndAddress);
                    }
                    return Mono.just(deliveryRequestAndAddress);
                })
                .flatMap(deliveryRequestAndAddress -> {
                    PnDeliveryRequest pnDeliveryRequest = deliveryRequestAndAddress.getT1();
                    Address address = deliveryRequestAndAddress.getT2();
                    this.sqsQueueSender.pushPrepareEvent(PrepareEventMapper.toPrepareEvent(pnDeliveryRequest, address, StatusCodeEnum.OK));

                    return this.requestDeliveryDAO.updateData(pnDeliveryRequest);
                })
                .onErrorResume(ex -> {
                    log.error("on Error : {}", ex.getMessage());
                    StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
                    if(ex instanceof PnGenericException) {
                        statusDeliveryEnum=mapper(((PnGenericException) ex).getExceptionType()) ;
                    }
                    updateStatus(requestId, correlationId, statusDeliveryEnum);
                    return Mono.error(ex);
                });
    }

    private StatusDeliveryEnum mapper(ExceptionTypeEnum ex){
        switch (ex){
            case UNTRACEABLE_ADDRESS : return StatusDeliveryEnum.UNTRACEABLE;
            default : return StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
        }

    }

    private void setLetterCode(Address address, PnDeliveryRequest deliveryRequest){
        //nazionale
        if(StringUtils.isNotBlank(address.getCap())){
            if(deliveryRequest.getProposalProductType().equals(RACCOMANDATA_SEMPLICE)){
                deliveryRequest.setProductType(ProductTypeEnum.RN_RS.getValue());
            }
            if(deliveryRequest.getProposalProductType().equals(RACCOMANDATA_890)){
                deliveryRequest.setProductType(ProductTypeEnum.RN_890.getValue());
            }
            if(deliveryRequest.getProposalProductType().equals(RACCOMANDATA_AR)){
                deliveryRequest.setProductType(ProductTypeEnum.RN_AR.getValue());
            }
        }
        //internazionale
        else{
            if(deliveryRequest.getProposalProductType().equals(RACCOMANDATA_SEMPLICE)){
                deliveryRequest.setProductType(ProductTypeEnum.RI_RS.getValue());
            }
            if(deliveryRequest.getProposalProductType().equals(RACCOMANDATA_890)){
                deliveryRequest.setProductType(ProductTypeEnum.RI_AR.getValue());
            }
            if(deliveryRequest.getProposalProductType().equals(RACCOMANDATA_AR)){
                deliveryRequest.setProductType(ProductTypeEnum.RI_AR.getValue());
            }
        }
    }

    public void updateStatus (String requestId, String correlationId, StatusDeliveryEnum status ){
        Mono<PnDeliveryRequest> pnDeliveryRequest;
        if (StringUtils.isNotEmpty(requestId) && !StringUtils.isNotEmpty(correlationId) ){
            pnDeliveryRequest= this.requestDeliveryDAO.getByRequestId(requestId);
        }else{
            pnDeliveryRequest= this.requestDeliveryDAO.getByCorrelationId(correlationId);
        }
        pnDeliveryRequest.map(
                entity -> {
                    entity.setStatusCode(status.getCode());
                    entity.setStatusDetail(status.getDescription());
                    entity.setStatusDate(DateUtils.formatDate(new Date()));
                    return this.requestDeliveryDAO.updateData(entity);
                }).block();
    }

    private Address setCorrectAddress(String requestId, String iun, String hashOldAddress, Address fromNationalRegistry, Address discoveredAddress) {

        //se nationalRegistry è diverso da null
        if(fromNationalRegistry != null){

            //indirizzo diverso da quello del primo invio?
            if(!fromNationalRegistry.convertToHash().equals(hashOldAddress)){
                String logMessage = String.format("prepare requestId = %s iun = %s and National Registry Address", requestId, iun);
                PnAuditLogEvent logEvent = auditLogBuilder
                        .before(PnAuditLogEventType.AUD_FD_RESOLVE_SERVICE, logMessage)
                        .build();
                logEvent.log();

                return fromNationalRegistry;
            }
            //indirizzo ricevuto in input
            else if(discoveredAddress!=null){
                String logMessage = String.format("prepare requestId = %s iun = %s and Discovered Address", requestId, iun);
                PnAuditLogEvent logEvent = auditLogBuilder
                        .before(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, logMessage)
                        .build();
                logEvent.log();

                return discoveredAddress;
            }
            //indirizzo non trovato
            else{
                String logMessage = String.format("prepare requestId = %s iun = %s Unreachable Address", requestId, iun);
                PnAuditLogEvent logEvent = auditLogBuilder
                        .before(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, logMessage)
                        .build();
                logEvent.generateFailure(logMessage);

                throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
            }
        }
        //indirizzo ricevuto in input
        else if(discoveredAddress!=null){
            String logMessage = String.format("prepare requestId = %s iun = %s and Discovered Address", requestId, iun);
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, logMessage)
                    .build();
            logEvent.log();

            return discoveredAddress;
        }
        //indirizzo non trovato
        else{
            String logMessage = String.format("prepare requestId = %s iun = %s Unreachable Address", requestId, iun);
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, logMessage)
                    .build();
            logEvent.generateFailure(logMessage);

            throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
        }
    }

    public Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis){
        if (n<0)
            return Mono.error(new PnGenericException( DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage() ) );
        else {
            return Mono.delay(Duration.ofMillis( millis.longValue() ))
                     .flatMap(item -> safeStorageClient.getFile(fileKey)
                     .map(fileDownloadResponseDto -> fileDownloadResponseDto)
                     .onErrorResume(ex -> {
                         log.error (ex.getMessage());
                         return Mono.error(ex);
                     })
                     .onErrorResume(PnRetryStorageException.class, ex ->
                         getFileRecursive(n - 1, fileKey, ex.getRetryAfter())
                    ));
        }
    }

    private Mono<PnDeliveryRequest> getAttachmentsInfo(PnDeliveryRequest deliveryRequest){

        if(deliveryRequest.getAttachments().isEmpty() ||
                !deliveryRequest.getAttachments().stream().filter(a ->a.getNumberOfPage()!=null && a.getNumberOfPage()>0).collect(Collectors.toList()).isEmpty()){
            return Mono.just(deliveryRequest);
        }

        return Flux.fromStream(deliveryRequest.getAttachments().stream())
                .parallel()
                .flatMap( attachment -> getFileRecursive(3, attachment.getFileKey(), new BigDecimal(0)))
                .flatMap(fileResponse -> {

                    AttachmentInfo info = AttachmentMapper.fromSafeStorage(fileResponse);
                    if (info.getUrl() == null)
                        throw new PnGenericException(DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage());
                    return HttpConnector.downloadFile(info.getUrl())
                            .map(pdDocument -> {
                                try {
                                    if (pdDocument.getDocumentInformation() != null && pdDocument.getDocumentInformation().getCreationDate() != null) {
                                        info.setDate(DateUtils.formatDate(pdDocument.getDocumentInformation().getCreationDate().getTime()));
                                    }
                                    info.setNumberOfPage(pdDocument.getNumberOfPages());
                                    pdDocument.close();
                                } catch (IOException e) {
                                    throw new PnGenericException(DOCUMENT_NOT_DOWNLOADED, DOCUMENT_NOT_DOWNLOADED.getMessage());
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
                });
    }



}
