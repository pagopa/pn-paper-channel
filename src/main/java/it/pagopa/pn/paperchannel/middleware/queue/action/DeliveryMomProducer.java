package it.pagopa.pn.paperchannel.middleware.queue.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class DeliveryMomProducer extends AbstractSqsMomProducer<DeliveryEvent> {

    public DeliveryMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<DeliveryEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
        log.info("DeliveryMomProducer");
    }
}
