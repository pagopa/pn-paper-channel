package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class InternalQueueMomProducer extends AttemptedQueueMomProducer<InternalPushEvent> {

    public InternalQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<InternalPushEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }

}
