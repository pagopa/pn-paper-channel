package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptPushEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class NormalizeAddressQueueMomProducer extends AttemptedQueueMomProducer<AttemptPushEvent> {

    public NormalizeAddressQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<AttemptPushEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }

}
