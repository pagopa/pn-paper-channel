package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.api.dto.events.PnPreparePaperchannelToDelayerEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class PaperchannelToDelayerMomProducer extends AbstractSqsMomProducer<PnPreparePaperchannelToDelayerEvent> {

    public PaperchannelToDelayerMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<PnPreparePaperchannelToDelayerEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }
}
