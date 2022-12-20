package it.pagopa.pn.paperchannel.service.impl;

import com.sun.jdi.LongValue;
import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
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
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

@Slf4j
@Service
public class PrepareAsyncServiceImpl implements PaperAsyncService {

    public static final String RACCOMANDATA_SEMPLICE = "Raccomandata semplice";
    public static final String RACCOMANDATA_890 = "Raccomandata 890";
    public static final String RACCOMANDATA_AR = "Raccomandata AR";

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private SafeStorageClient safeStorageClient;
    @Autowired
    private AddressDAO addressDAO;


    @Override
    public Mono<DeliveryAsyncModel> prepareAsync(String requestId, String correlationId, Address address){

        Mono<PnDeliveryRequest> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
        if(correlationId!= null){
            requestDeliveryEntityMono = requestDeliveryDAO.getByCorrelationId(correlationId);
        }

        return requestDeliveryEntityMono
                .flatMap(requestDeliveryEntity -> {
                    DeliveryAsyncModel deliveryAsyncModel = new DeliveryAsyncModel();
                    deliveryAsyncModel.setRequestId(requestDeliveryEntity.getRequestId());

                    if(requestDeliveryEntity.getAttachments()!=null){
                        deliveryAsyncModel.setAttachments(requestDeliveryEntity.getAttachments().stream()
                                .map(AttachmentMapper::fromEntity).collect(Collectors.toList()));
                    }
                    //settare address se not null
                    setLetterCode(deliveryAsyncModel,requestDeliveryEntity.getRegisteredLetterCode());
                    return Mono.just(deliveryAsyncModel);
                })
                .flatMap(deliveryAsyncModel -> getAttachmentsInfo(deliveryAsyncModel).map(newModel -> newModel))
                .flatMap(deliveryAsyncModel -> getAmount(deliveryAsyncModel).map(newModel -> newModel))
                .flatMap(deliveryAsyncModel -> addressDAO.create(AddressMapper.toEntity(address, deliveryAsyncModel.getRequestId()))
                        .map(item -> {
                            deliveryAsyncModel.setAddress(AddressMapper.toDTO(item));
                            return deliveryAsyncModel;
                        }));
    }

    private Mono<DeliveryAsyncModel> setLetterCode(DeliveryAsyncModel deliveryAsyncModel, String registerLetterCode){
        //nazionale
        if(deliveryAsyncModel.getAddress().getCap()!=null){
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
            if(registerLetterCode.equals(RACCOMANDATA_AR)){
                deliveryAsyncModel.setProductType(ProductTypeEnum.RI_AR);
            }
        }
        return Mono.just(deliveryAsyncModel);
    }

    private Mono<DeliveryAsyncModel> getAmount(DeliveryAsyncModel deliveryAsyncModel){
        return getContract("","")
                .flatMap(contract -> getPriceAttachments(deliveryAsyncModel, contract.getPricePerPage())
                        .map(priceForPages -> Double.sum(contract.getPrice(), priceForPages))
                )
                .map(amount ->{
                    deliveryAsyncModel.setAmount(amount);
                    return deliveryAsyncModel;
                });
    }

    private Mono<DeliveryAsyncModel> getContractAddress(DeliveryAsyncModel model, Address fromNationalRegistry, Address address) {

        //se nationalRegistry è diverso da null
        if(fromNationalRegistry!=null){

            //indirizzo diverso da quello del primo invio?
            if(!fromNationalRegistry.convertToHash().equals(model.getHashOldAddress())){
                model.setAddress(fromNationalRegistry);
            }
            //indirizzo ricevuto in input
            else if(address!=null){
                model.setAddress(address);
            }
            //indirizzo non trovato
            else{
                throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
            }
        }
        //indirizzo ricevuto in input
        else if(address!=null){
            model.setAddress(address);
        }
        //indirizzo non trovato
        else{
            throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
        }
        return Mono.just(model);
    }

    private Mono<Contract> getContract(String capOrZone, String registerLetter) {
        return Mono.just(new Contract(5.0, 10.0));
    }



    public Mono<FileDownloadResponseDto> getFileRecursive(Integer n, String fileKey, BigDecimal millis){
        if (n<0)
            return Mono.error(new PnGenericException( DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage() ) );
        else {
             return Mono.just ("").delay(Duration.ofMillis( millis.longValue() ))
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

    private Mono<Double> getPriceAttachments(DeliveryAsyncModel deliveryAsyncModel, Double priceForAAr){
        return Flux.fromStream(deliveryAsyncModel.getAttachments().stream())
                .map(attachmentInfo -> attachmentInfo.getNumberOfPage() * priceForAAr)
                .reduce(0.0, Double::sum);
    }

}
