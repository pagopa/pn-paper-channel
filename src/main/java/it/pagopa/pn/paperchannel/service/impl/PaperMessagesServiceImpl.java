package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;

import it.pagopa.pn.paperchannel.mapper.PreparePaperResponseMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.pojo.Address;
import it.pagopa.pn.paperchannel.pojo.AttachmentInfo;
import it.pagopa.pn.paperchannel.pojo.Contract;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@Slf4j
@Service
public class PaperMessagesServiceImpl implements PaperMessagesService {
    //@Value("${}")
    //private String attemps;
    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private SafeStorageClient safeStorageClient;
    @Autowired
    private NationalRegistryClient nationalRegistryClient;

    @Override
    public Mono<SendEvent> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        return requestDeliveryDAO.getByRequestId(requestId)

                // Case of 200
                .map(PreparePaperResponseMapper::fromResult)
                .onErrorResume(PnGenericException.class, ex -> {
                    if (ex.getExceptionType() == DELIVERY_REQUEST_NOT_EXIST){
                        log.info("Delivery request");
                        return requestDeliveryDAO.create(RequestDeliveryMapper.toEntity(prepareRequest))
                                .map(entity -> {
                                    // Case of 204
                                    log.info("Entity creata");
                                    Mono.just("")
                                            .publishOn(Schedulers.parallel())
                                            .flatMap(item -> prepareAsync(prepareRequest))
                                            .subscribe(new SubscriberPrepare(null));
                                    throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(requestId));
                                });
                    }
                    return Mono.error(ex);
                });

    }

    //TODO aggiungere metodo per confrontare PrepareRequest con RequestDeliveryEntity, In caso,
    // requestId uguali ma dati, come inidirizzo, differenti allora 409


    private Mono<DeliveryPayload> prepareAsync(PrepareRequest request){

        return getAddress(request)
            .zipWhen(address -> {
                return getContract()
                    .flatMap(contract ->
                        getPriceAttachments(request, contract.getPricePerPage())
                                .map(price -> Double.sum(contract.getPrice(), price)) );
                    })
                .map((tupla) -> {
                    return new DeliveryPayload(tupla.getT1(), tupla.getT2());
                });
    }

    private Mono<Contract> getContract() {
        return Mono.just(new Contract(5.0, 10.0));
    }

    private Mono<Double> getPriceAttachments(PrepareRequest prepareRequest, Double priceForAAr){
        return getAttachmentsInfo(prepareRequest)
                .map(attachmentInfo -> attachmentInfo.getNumberOfPage() * priceForAAr)
                .sequential()
                .reduce(0.0, Double::sum);
    }

    private Mono<Address> getAddress(PrepareRequest prepareRequest){
        if (prepareRequest.getReceiverAddress() != null){
            return Mono.just(AddressMapper.fromAnalogToAddress(prepareRequest.getReceiverAddress()));
        }
        if (prepareRequest.getDiscoveredAddress() != null){
            return Mono.just(AddressMapper.fromAnalogToAddress(prepareRequest.getDiscoveredAddress()));
        }

        return nationalRegistryClient.finderAddress(prepareRequest.getReceiverFiscalCode())
                .map(AddressMapper::fromNationalRegistry)
                //TODO Gestire caso di irreperibile totale.
                .onErrorResume(PnGenericException.class, Mono::error);
    }

    private ParallelFlux<AttachmentInfo> getAttachmentsInfo(PrepareRequest prepareRequest){
        return Flux.fromStream(prepareRequest.getAttachmentUrls().stream())
                .parallel()
                .flatMap(fileKey -> safeStorageClient.getFile(fileKey)
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



}
