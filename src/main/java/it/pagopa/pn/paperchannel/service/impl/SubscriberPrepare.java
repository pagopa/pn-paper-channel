package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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
            /*
            if(exception.getExceptionType().equals(ExceptionTypeEnum.UNTRACEABLE_ADDRESS)){
                //Mono<RequestDeliveryEntity> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
                //Aggiornare o inserire entity per etichettare codice fiscale come irreperibile totale
            }
            if(exception.getExceptionType().equals(ExceptionTypeEnum.DOCUMENT_URL_NOT_FOUND)){
                //Mono<RequestDeliveryEntity> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
                //Aggiornare o inserire entity per etichettare codice fiscale come irreperibile totale
            }
            if(exception.getExceptionType().equals(ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED)){
                //Mono<RequestDeliveryEntity> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
                //Aggiornare o inserire entity per etichettare codice fiscale come irreperibile totale
            }
            if(exception.getExceptionType().equals(ExceptionTypeEnum.RETRY_AFTER_DOCUMENT)){
                //Mono<RequestDeliveryEntity> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);
                //Aggiornare o inserire entity per etichettare codice fiscale come irreperibile totale
            }
            */
        }
    }

    @Override
    public void onComplete() {
        log.info("Custom subscriber on complete");

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
