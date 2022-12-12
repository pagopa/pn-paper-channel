package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.AddressEntity;
import it.pagopa.pn.paperchannel.middleware.db.entities.AttachmentInfoEntity;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.pojo.Address;
import it.pagopa.pn.paperchannel.pojo.AttachmentInfo;
import it.pagopa.pn.paperchannel.pojo.Contract;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
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
    public Mono<DeliveryPayload> prepareAsync(String requestId, String correlationId, Address address){

        Mono<RequestDeliveryEntity> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
        if(correlationId!= null){
            requestDeliveryEntityMono = requestDeliveryDAO.getByCorrelationId(correlationId);
        }
        return requestDeliveryEntityMono
                .zipWhen(requestDeliveryEntity -> addressDAO.create(AddressMapper.toEntity( address,requestDeliveryEntity.getRequestId()))
                        .map(item -> item)
                )
                .flatMap(requestAndAddress -> getDeliveryPayload(requestAndAddress.getT1(),requestAndAddress.getT2())
                        .map(deliveryPayload -> deliveryPayload)
                );
    }

    private Mono<DeliveryPayload> getDeliveryPayload(RequestDeliveryEntity request, AddressEntity address){
        log.info("Start Prepare Async");
        return getContract()
                .flatMap(contract -> getPriceAttachments(request.getAttachments(), contract.getPricePerPage())
                        .map(price -> Double.sum(contract.getPrice(), price))
                )
                .map(price -> new DeliveryPayload(AddressMapper.toDTO(address), price));
    }

    private Mono<Contract> getContract() {
        return Mono.just(new Contract(5.0, 10.0));
    }

    private ParallelFlux<AttachmentInfo> getAttachmentsInfo(List<AttachmentInfoEntity> attachmentsFileKey){
        return Flux.fromStream(attachmentsFileKey.stream())
                .parallel()
                .flatMap(attachment -> safeStorageClient.getFile(attachment.getFileKey())
                        .retryWhen(
                                Retry.backoff(3, Duration.ofMillis(500))
                                        .filter(PnRetryStorageException.class::isInstance)
                        )
                )
                .map(fileResponse -> {
                    try {
                        AttachmentInfo info = AttachmentMapper.fromSafeStorage(fileResponse);
                        if (info.getUrl() == null)
                            throw new PnGenericException(DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage());
                        PDDocument pdDocument = HttpConnector.downloadFile(info.getUrl());
                        info.setDate(DateUtils.formatDate(pdDocument.getDocumentInformation().getCreationDate().getTime()));
                        info.setNumberOfPage(pdDocument.getNumberOfPages());
                        return info;
                    } catch (IOException e) {
                        throw new PnGenericException(DOCUMENT_NOT_DOWNLOADED, DOCUMENT_NOT_DOWNLOADED.getMessage());
                    }
                });
    }

    private Mono<Double> getPriceAttachments(List<AttachmentInfoEntity> attachmentsFileKey, Double priceForAAr){
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
