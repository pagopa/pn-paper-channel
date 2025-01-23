package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptPushEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DelayerToPaperChannelQueueMomProducer extends AttemptedQueueMomProducer<PnPrepareDelayerToPaperchannelEvent> {
    public DelayerToPaperChannelQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper,
                                          Class<PnPrepareDelayerToPaperchannelEvent> msgClass)  {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
