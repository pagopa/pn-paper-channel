package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

@Slf4j
public class SubscriberPrepare implements Subscriber<DeliveryPayload> {

    private DeliveryPayload deliveryPayload;
    private SqsQueueSender sqsQueueSender;
    private RequestDeliveryDAO requestDeliveryDAO;
    private String requestId;

    public SubscriberPrepare(SqsQueueSender sqsQueueSender, String requestId, RequestDeliveryDAO requestDeliveryDAO) {
        this.sqsQueueSender = sqsQueueSender;
        this.requestId = requestId;
        this.requestDeliveryDAO = requestDeliveryDAO;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(1);
    }

    @Override
    public void onNext(DeliveryPayload deliveryPayload) {
        this.deliveryPayload = deliveryPayload;
        log.info("Custom subscriber on next");
        log.info(deliveryPayload.toString());
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("on Error : {}", throwable.getMessage());
        if(throwable instanceof PnGenericException){
            PnGenericException exception = (PnGenericException) throwable;
            if(exception.getExceptionType().equals(ExceptionTypeEnum.UNTRACEABLE_ADDRESS)){
                Mono<RequestDeliveryEntity> requestDeliveryEntityMono = requestDeliveryDAO.getByRequestId(requestId);

            }
        }
    }

    @Override
    public void onComplete() {
        log.info("Custom subscriber on complete");
    }
}
