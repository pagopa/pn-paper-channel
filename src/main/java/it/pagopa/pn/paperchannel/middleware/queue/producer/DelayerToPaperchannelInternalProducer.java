package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptPushEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Producer used to perform PREPARE phase two redrive in case of flow 24 or errors
 */
public class DelayerToPaperchannelInternalProducer extends AttemptedQueueMomProducer<AttemptPushEvent> {

    public DelayerToPaperchannelInternalProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<AttemptPushEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
