package it.pagopa.pn.paperchannel.service.impl;


import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class SubscriberPrepare implements Subscriber<DeliveryPayload> {

    private DeliveryPayload deliveryPayload;
    private SqsQueueSender sqsQueueSender;

    public SubscriberPrepare(SqsQueueSender sqsQueueSender) {
        this.sqsQueueSender = sqsQueueSender;
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
    }

    @Override
    public void onComplete() {
        log.info("Custom subscriber on complete");
    }
}
