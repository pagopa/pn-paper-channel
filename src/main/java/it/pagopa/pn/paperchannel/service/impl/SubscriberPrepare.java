package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.pojo.DeliveryAsyncModel;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class SubscriberPrepare implements Subscriber<DeliveryAsyncModel> {

    private DeliveryAsyncModel deliveryAsyncModel;
    private final SqsQueueSender sqsQueueSender;
    private final RequestDeliveryDAO requestDeliveryDAO;
    private final String requestId;
    private final String corralationId;

    public SubscriberPrepare(SqsQueueSender sqsQueueSender, RequestDeliveryDAO requestDeliveryDAO, String requestId, String corralationId) {
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
        //controllo se valorizzati

        //fare query per recuperare i dati dalla tabella e tutti gli altri ogetti

        //push
        log.info("Custom subscriber on complete");
    }
}
