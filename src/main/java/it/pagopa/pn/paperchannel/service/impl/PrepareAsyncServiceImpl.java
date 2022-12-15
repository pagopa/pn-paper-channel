package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.RETRY_AFTER_DOCUMENT;

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

        Mono<RequestDeliveryEntity> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
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
                .flatMap(deliveryAsyncModel -> addressDAO.create(AddressMapper.toEntity( address, deliveryAsyncModel.getRequestId()))
                                    .map(item -> {
                                        deliveryAsyncModel.setAddress(AddressMapper.toDTO(item));
                                        return deliveryAsyncModel;
                                    })
                ).flatMap(deliveryAsyncModel -> getAmount(deliveryAsyncModel).map(item -> item));
    }

    private Mono<DeliveryAsyncModel> getAmount(DeliveryAsyncModel deliveryAsyncModel){
        return getContract()
                .flatMap(contract -> getPriceAttachments(deliveryAsyncModel.getAttachments(), contract.getPricePerPage())
                        .map(price -> Double.sum(contract.getPrice(), price))
                )
                .map(amount ->{
                    deliveryAsyncModel.setAmount(amount);
                    return deliveryAsyncModel;
                });
    }

    private Mono<Contract> getContract() {
        return Mono.just(new Contract(5.0, 10.0));
    }

    private ParallelFlux<AttachmentInfo> getAttachmentsInfo(List<AttachmentInfo> attachmentsFileKey){
        return Flux.fromStream(attachmentsFileKey.stream())
                .parallel()
                .flatMap(attachment -> safeStorageClient.getFile(attachment.getFileKey())
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(500))
                                        .filter(PnRetryStorageException.class::isInstance)
                        )
                )
                .map(fileResponse -> {
                  //  try {
                        AttachmentInfo info = AttachmentMapper.fromSafeStorage(fileResponse);
                        if (info.getUrl() == null)
                            throw new PnGenericException(DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage());
                      //  PDDocument pdDocument = HttpConnector.downloadFile(info.getUrl());
                      //  info.setDate(DateUtils.formatDate(pdDocument.getDocumentInformation().getCreationDate().getTime()));
                       // info.setNumberOfPage(pdDocument.getNumberOfPages());
                        return info;
                 //   } catch (IOException e) {
                   //     throw new PnGenericException(DOCUMENT_NOT_DOWNLOADED, DOCUMENT_NOT_DOWNLOADED.getMessage());
                    //}
                });
    }

    private Mono<Double> getPriceAttachments(List<AttachmentInfo> attachmentsFileKey, Double priceForAAr){
        return getAttachmentsInfo(attachmentsFileKey)
                .map(attachmentInfo -> attachmentInfo.getNumberOfPage() * priceForAAr)
                .sequential()
                .reduce(0.0, Double::sum)
                .onErrorResume(exception -> {
                    if(exception instanceof PnGenericException) {
                        return Mono.error(exception);
                    }
                    if(exception instanceof PnRetryStorageException) {
                        return Mono.error(new PnGenericException(RETRY_AFTER_DOCUMENT, RETRY_AFTER_DOCUMENT.getMessage()));
                    }
                    return Mono.error(exception);
                });
    }

}
