package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class DeliveryPushMomProducer extends AbstractSqsMomProducer<DeliveryPushEvent> {

    public DeliveryPushMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<DeliveryPushEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
