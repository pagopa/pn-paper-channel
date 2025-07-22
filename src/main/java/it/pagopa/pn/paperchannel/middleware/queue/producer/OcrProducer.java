package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.OcrInputEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class OcrProducer extends AbstractSqsMomProducer<OcrInputEvent> {

    public OcrProducer(SqsClient sqsClient, String topic, String queueUrl, ObjectMapper objectMapper, Class<OcrInputEvent> msgClass) {
        super(sqsClient, topic, queueUrl, objectMapper, msgClass);
    }
}
