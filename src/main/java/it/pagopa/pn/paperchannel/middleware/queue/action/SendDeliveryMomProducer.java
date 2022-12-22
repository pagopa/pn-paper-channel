package it.pagopa.pn.paperchannel.middleware.queue.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.SendDeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class SendDeliveryMomProducer extends AbstractSqsMomProducer<SendDeliveryEvent> {



    public SendDeliveryMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<SendDeliveryEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }


}
