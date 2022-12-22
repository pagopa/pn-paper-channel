package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPayload;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
public class SubscriberPrepare implements Subscriber<DeliveryAsyncModel> {

    private DeliveryAsyncModel deliveryAsyncModel;
    private final SqsSender sqsQueueSender;
    private final RequestDeliveryDAO requestDeliveryDAO;
    private final String requestId;
    private final String corralationId;

    public SubscriberPrepare(SqsSender sqsQueueSender, RequestDeliveryDAO requestDeliveryDAO, String requestId, String corralationId) {
        this.sqsQueueSender = sqsQueueSender;
        this.requestDeliveryDAO = requestDeliveryDAO;
        this.requestId = requestId;
        this.corralationId = corralationId;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(1);
    }

    @Override
    public void onNext(DeliveryAsyncModel deliveryAsyncModel) {
        this.deliveryAsyncModel = deliveryAsyncModel;
        log.info("Custom subscriber on next");
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("on Error : {}", throwable.getMessage());
        if(throwable instanceof PnGenericException){
            PnGenericException exception = (PnGenericException) throwable;
            if(exception.getExceptionType().equals(ExceptionTypeEnum.UNTRACEABLE_ADDRESS)
            || exception.getExceptionType().equals(ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND)
            || exception.getExceptionType().equals(ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED)
            || exception.getExceptionType().equals(ExceptionTypeEnum.RETRY_AFTER_DOCUMENT)
            )
             updatentity();
        }
    }

    public void updatentity(){
        Address address = null;
        Mono<PnDeliveryRequest> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
        //todo inserire codice fiscale come irreperibile
        //Aggiornare o inserire entity per etichettare codice fiscale come irreperibile totale
        requestDeliveryDAO.updateData( Mono.just(requestDeliveryEntityMono)  );
    }

    @Override
    public void onComplete() {
        log.info("Custom subscriber on complete");
        DeliveryPayload payload = new DeliveryPayload();

        PrepareEvent prepareEvent = new PrepareEvent();
        sqsQueueSender.pushPrepareEvent(prepareEvent);

        requestDeliveryDAO.getByRequestId(deliveryAsyncModel.getRequestId())
                .mapNotNull(requestDeliveryEntity -> {

                    requestDeliveryEntity.setStatusCode(StatusDeliveryEnum.TAKING_CHARGE.getCode());
                    requestDeliveryEntity.setStatusDetail(StatusDeliveryEnum.TAKING_CHARGE.getDescription());
                    requestDeliveryEntity.setStatusDate(DateUtils.formatDate(new Date()));
                    requestDeliveryEntity.setAttachments(deliveryAsyncModel.getAttachments().stream()
                            .map(AttachmentMapper::toEntity).collect(Collectors.toList()));
                    requestDeliveryEntity.setProductType(deliveryAsyncModel.getProductType().getValue());
                    return requestDeliveryDAO.updateData(requestDeliveryEntity).map(item->item);
                }).subscribe();
    }
}
