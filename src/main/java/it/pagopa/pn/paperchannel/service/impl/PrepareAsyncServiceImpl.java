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
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

@Slf4j
@Service
public class PrepareAsyncServiceImpl implements PaperAsyncService {

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

    private Mono<DeliveryAsyncModel> getAttachmentsInfo(DeliveryAsyncModel deliveryAsyncModel){

        if(deliveryAsyncModel.getAttachments().isEmpty() ||
                !deliveryAsyncModel.getAttachments().stream().filter(a -> a.getNumberOfPage()>0).collect(Collectors.toList()).isEmpty()){
            return Mono.just(deliveryAsyncModel);
        }

        return Flux.fromStream(deliveryAsyncModel.getAttachments().stream())
                .parallel()
                .flatMap(attachment -> safeStorageClient.getFile(attachment.getFileKey())
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(500))
                                        .filter(PnRetryStorageException.class::isInstance)
                        )
                )
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
