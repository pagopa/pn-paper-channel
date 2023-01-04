package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

@Slf4j
@Service
public class PrepareAsyncServiceImpl extends BaseService implements PaperAsyncService {

    public static final String RACCOMANDATA_SEMPLICE = "RS";
    public static final String RACCOMANDATA_890 = "890";
    public static final String RACCOMANDATA_AR = "AR";

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private SafeStorageClient safeStorageClient;
    @Autowired
    private AddressDAO addressDAO;


    @Override
    public Mono<DeliveryAsyncModel> prepareAsync(String requestId, String correlationId, Address addressFromNationalRegistry){
        log.info("Start async");
        Mono<PnDeliveryRequest> requestDeliveryEntityMono =null;
        if(correlationId!= null)
            requestDeliveryEntityMono = requestDeliveryDAO.getByCorrelationId(correlationId);
        else
            requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);

        return requestDeliveryEntityMono
                .zipWhen(entity -> addressDAO.findByRequestId(entity.getRequestId()).map(item->item))
                .flatMap(entityAndAddress -> {
                    PnDeliveryRequest pnDeliveryRequest = entityAndAddress.getT1();
                    // Sarà diverso da null solo nel secondo tentativo
                    Address fromDB = AddressMapper.toDTO(entityAndAddress.getT2());

                    //Creo model
                    DeliveryAsyncModel deliveryAsyncModel = new DeliveryAsyncModel();
                    deliveryAsyncModel.setRequestId(pnDeliveryRequest.getRequestId());

                    //setto gli allegati se ci sono
                    if(pnDeliveryRequest.getAttachments() != null){
                        deliveryAsyncModel.setAttachments(pnDeliveryRequest.getAttachments().stream()
                                .map(AttachmentMapper::fromEntity).collect(Collectors.toList()));
                    }

                    deliveryAsyncModel.setHashOldAddress(pnDeliveryRequest.getAddressHash());

                    if (StringUtils.isNotBlank(correlationId)){
                        /*
                        se siamo nel secondo tentativo dobbiamo fare i controlli su:
                            - indirizzo recuperato da National Registry
                            - hash indirizzo primo tentativo
                            - indirizzo scoperto dal postino se != null
                        */
                        setCorrectAddress(deliveryAsyncModel, addressFromNationalRegistry, fromDB);
                    } else {
                        // altrimenti ci troviamo nel primo tentativo dove fromDB sempre != null
                        deliveryAsyncModel.setAddress(fromDB);
                    }

                    if (pnDeliveryRequest.getProductType() == null ) {
                        setLetterCode(deliveryAsyncModel, pnDeliveryRequest.getProposalProductType());
                    } else {
                        deliveryAsyncModel.setProductType(ProductTypeEnum.fromValue(pnDeliveryRequest.getProductType()));
                    }
                    return Mono.just(deliveryAsyncModel).delayElement(Duration.ofMillis(2000));
                })

                .flatMap(deliveryAsyncModel -> getAttachmentsInfo(deliveryAsyncModel).map(newModel -> newModel))
                .flatMap(deliveryAsyncModel -> {
                    if (deliveryAsyncModel.isFromNationalRegistry()){
                        return addressDAO.create(AddressMapper.toEntity(addressFromNationalRegistry, deliveryAsyncModel.getRequestId()))
                                .map(item -> deliveryAsyncModel);
                    }
                    return Mono.just(deliveryAsyncModel);
                });
    }

    private void setLetterCode(DeliveryAsyncModel deliveryAsyncModel, String registerLetterCode){
        //nazionale
        if(StringUtils.isNotBlank(deliveryAsyncModel.getAddress().getCap())){
            if(registerLetterCode.equals(RACCOMANDATA_SEMPLICE)){
                deliveryAsyncModel.setProductType(ProductTypeEnum.RN_RS);
            }
            if(registerLetterCode.equals(RACCOMANDATA_890)){
                deliveryAsyncModel.setProductType(ProductTypeEnum.RN_890);
            }
            if(registerLetterCode.equals(RACCOMANDATA_AR)){
                deliveryAsyncModel.setProductType(ProductTypeEnum.RN_AR);
            }
        }
        //internazionale
        else{
            if(registerLetterCode.equals(RACCOMANDATA_SEMPLICE)){
                deliveryAsyncModel.setProductType(ProductTypeEnum.RI_RS);
            }
            if(registerLetterCode.equals(RACCOMANDATA_890)){
                deliveryAsyncModel.setProductType(ProductTypeEnum.RI_AR);
            }
            if(registerLetterCode.equals(RACCOMANDATA_AR)){
                deliveryAsyncModel.setProductType(ProductTypeEnum.RI_AR);
            }
        }
    }

    private void setCorrectAddress(DeliveryAsyncModel model, Address fromNationalRegistry, Address discoveredAddress) {

        //se nationalRegistry è diverso da null
        if(fromNationalRegistry != null){

            //indirizzo diverso da quello del primo invio?
            if(!fromNationalRegistry.convertToHash().equals(model.getHashOldAddress())){
                model.setFromNationalRegistry(true);
                model.setAddress(fromNationalRegistry);
            }
            //indirizzo ricevuto in input
            else if(discoveredAddress!=null){
                model.setAddress(discoveredAddress);
            }
            //indirizzo non trovato
            else{
                throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
            }
        }
        //indirizzo ricevuto in input
        else if(discoveredAddress!=null){
            model.setAddress(discoveredAddress);
        }
        //indirizzo non trovato
        else{
            throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
        }
    }

    public Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis){
        if (n<0)
            return Mono.error(new PnGenericException( DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage() ) );
        else {
             return Mono.just ("").delay(Duration.ofMillis( millis.longValue() ))
                     .flatMap(item -> safeStorageClient.getFile(fileKey)
                     .map(fileDownloadResponseDto -> {
                                log.debug("Url file "+fileDownloadResponseDto.getDownload().getUrl());
                                 return fileDownloadResponseDto;
                     })
                     .onErrorResume(ex -> {
                         log.error (ex.getMessage());
                         return Mono.error(ex);
                     })
                     .onErrorResume(PnRetryStorageException.class, ex ->
                         getFileRecursive(n - 1, fileKey, ex.getRetryAfter())
                    ));
        }
    }

    private Mono<DeliveryAsyncModel> getAttachmentsInfo(DeliveryAsyncModel deliveryAsyncModel){

        if(deliveryAsyncModel.getAttachments().isEmpty() ||
                !deliveryAsyncModel.getAttachments().stream().filter(a -> a.getNumberOfPage()>0).collect(Collectors.toList()).isEmpty()){
            return Mono.just(deliveryAsyncModel);
        }

        return Flux.fromStream(deliveryAsyncModel.getAttachments().stream())
                .parallel()
                .flatMap( attachment -> getFileRecursive(3, attachment.getFileKey(), new BigDecimal(0)))
                .flatMap(fileResponse -> {

                    AttachmentInfo info = AttachmentMapper.fromSafeStorage(fileResponse);
                    if (info.getUrl() == null)
                        throw new PnGenericException(DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage());
                    return HttpConnector.downloadFile(info.getUrl())
                            .map(pdDocument -> {
                                try {
                                info.setDate(DateUtils.formatDate(pdDocument.getDocumentInformation().getCreationDate().getTime()));
                                info.setNumberOfPage(pdDocument.getNumberOfPages());
                                    pdDocument.close();
                                } catch (IOException e) {
                                    throw new PnGenericException(DOCUMENT_NOT_DOWNLOADED, DOCUMENT_NOT_DOWNLOADED.getMessage());
                                }
                                return info;
                            });

                })
                .sequential()
                .collectList()
                .map(listAttachment -> {
                    deliveryAsyncModel.setAttachments(listAttachment);
                    return deliveryAsyncModel;
                });
    }

}
