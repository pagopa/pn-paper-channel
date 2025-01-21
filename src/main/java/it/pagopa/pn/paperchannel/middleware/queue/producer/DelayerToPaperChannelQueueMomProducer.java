package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.middleware.queue.model.DelayerToPaperChannelEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DelayerToPaperChannelQueueMomProducer extends AttemptedQueueMomProducer<DelayerToPaperChannelEvent> {
    public DelayerToPaperChannelQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper,
                                          Class<DelayerToPaperChannelEvent> msgClass)  {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
