package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.PreparePaperResponseMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.RetrivePrepareResponseMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.RETRY_AFTER_DOCUMENT;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

@Slf4j
@Service
public class PaperMessagesServiceImpl implements PaperMessagesService {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private SafeStorageClient safeStorageClient;
    @Autowired
    private NationalRegistryClient nationalRegistryClient;

    @Override
    public Mono<SendEvent> preparePaperSync(String requestId, PrepareRequest prepareRequest){

        return requestDeliveryDAO.getByRequestId(requestId)
                // Case of 409
                .map(entity -> compareRequestEntity(prepareRequest, entity))
                // Case of 200,
                .map(PreparePaperResponseMapper::fromResult)
                .onErrorResume(PnGenericException.class, ex -> {
                    if (ex.getExceptionType() == DELIVERY_REQUEST_NOT_EXIST){
                        log.info("Delivery request");
                        prepareRequest.setRequestId(requestId);
                        return requestDeliveryDAO.create(RequestDeliveryMapper.toEntity(prepareRequest))
                                .map(entity -> {
                                    // Case of 204
                                    log.info("Entity creata");
                                    Mono.just("")
                                            .publishOn(Schedulers.parallel())
                                            .flatMap(item -> prepareAsync(prepareRequest))
                                            .subscribe(new SubscriberPrepare(null, requestId, requestDeliveryDAO));
                                    throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(requestId));
                                });
                    }
                    if (ex.getExceptionType() == DIFFERENT_DATA_REQUEST) {

                        return Mono.error(ex);
                    }
                    return Mono.error(ex);
                });

    }

    @Override
    public Mono<PrepareEvent> retrivePaperPrepareRequest(String requestId) {
        return requestDeliveryDAO.getByRequestId(requestId)
                .map(RetrivePrepareResponseMapper::fromResult);
    }


    //TODO aggiungere metodo per confrontare PrepareRequest con RequestDeliveryEntity, In caso,
    // requestId uguali ma dati, come inidirizzo, differenti allora 409


    private Mono<DeliveryPayload> prepareAsync(PrepareRequest request){
        log.info("Start Prepare Async");
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
                .onErrorResume(PnGenericException.class, exception ->
                        Mono.error(new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage())));
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


    private RequestDeliveryEntity compareRequestEntity(PrepareRequest prepareRequest, RequestDeliveryEntity requestDeliveryEntity) {
        if((prepareRequest.getRequestId().equals(requestDeliveryEntity.getRequestId()) &&
                (!(prepareRequest.getReceiverFiscalCode().equals(requestDeliveryEntity.getFiscalCode()))) ||
                (!(prepareRequest.getProductType().equals(requestDeliveryEntity.getRegisteredLetterCode()))))){

            //caso in cui recivered address è popolato mentre discovered address no
            if(((prepareRequest.getReceiverAddress()!=null && prepareRequest.getDiscoveredAddress()==null) ||
                    (prepareRequest.getReceiverAddress()==null && prepareRequest.getDiscoveredAddress()!=null)) &&
                    checkAddressInfo(prepareRequest,requestDeliveryEntity)){
                //recivered address diverso da quello precedentemente ricevuto
                throw new PnGenericException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT);
            }
            //caso in cui sono entrambi null l'indirizzo viene recuperato dal national registry
            if(prepareRequest.getReceiverAddress()==null && prepareRequest.getDiscoveredAddress()==null){
                throw new PnGenericException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT);
            }
        }
        return requestDeliveryEntity;
    }

    public boolean checkAddressInfo(PrepareRequest prepareRequest, RequestDeliveryEntity requestDeliveryEntity){

        return (!prepareRequest.getReceiverAddress().getAddress().equals(requestDeliveryEntity.getAddress().getAddress()) ||
                !prepareRequest.getReceiverAddress().getFullname().equals(requestDeliveryEntity.getAddress().getFullName()) ||
                !prepareRequest.getReceiverAddress().getNameRow2().equals(requestDeliveryEntity.getAddress().getNameRow2()) ||
                !prepareRequest.getReceiverAddress().getAddressRow2().equals(requestDeliveryEntity.getAddress().getAddressRow2()) ||
                !prepareRequest.getReceiverAddress().getCap().equals(requestDeliveryEntity.getAddress().getCap()) ||
                !prepareRequest.getReceiverAddress().getCity().equals(requestDeliveryEntity.getAddress().getCity()) ||
                !prepareRequest.getReceiverAddress().getCity2().equals(requestDeliveryEntity.getAddress().getCity2()) ||
                !prepareRequest.getReceiverAddress().getPr().equals(requestDeliveryEntity.getAddress().getPr()) ||
                !prepareRequest.getReceiverAddress().getCountry().equals(requestDeliveryEntity.getAddress().getCountry()));
    }
}
