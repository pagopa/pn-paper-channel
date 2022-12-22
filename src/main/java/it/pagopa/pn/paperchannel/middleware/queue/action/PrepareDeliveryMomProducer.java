package it.pagopa.pn.paperchannel.middleware.queue.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.PrepareDeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class PrepareDeliveryMomProducer extends AbstractSqsMomProducer<PrepareDeliveryEvent> {

    public PrepareDeliveryMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<PrepareDeliveryEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
