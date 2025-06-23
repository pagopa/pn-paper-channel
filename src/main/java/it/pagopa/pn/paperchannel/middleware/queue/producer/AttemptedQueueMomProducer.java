package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptEventHeader;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

public abstract class AttemptedQueueMomProducer<T extends GenericEvent> extends AbstractSqsMomProducer<T> {

    protected AttemptedQueueMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<T> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }


    @Override
    protected Map<String, MessageAttributeValue> getSqSHeader(GenericEventHeader header) {
        Map<String, MessageAttributeValue> map = super.getSqSHeader(header);
        if (!(header instanceof AttemptEventHeader headerCustom)) {
            return map;
        }
        map.put("attempt", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(String.valueOf(headerCustom.getAttempt()))
                .build());
        return map;
    }

}
