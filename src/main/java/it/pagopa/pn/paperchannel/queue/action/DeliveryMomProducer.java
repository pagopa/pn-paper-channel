package it.pagopa.pn.paperchannel.queue.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.paperchannel.queue.model.DeliveryEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DeliveryMomProducer extends AbstractSqsMomProducer<DeliveryEvent> {

    public DeliveryMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<DeliveryEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
