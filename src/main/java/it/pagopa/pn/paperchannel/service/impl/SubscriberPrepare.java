package it.pagopa.pn.paperchannel.service.impl;


import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}
