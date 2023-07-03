package it.pagopa.pn.paperchannel.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.sqs.SqsClient;

@ActiveProfiles("local")
@Configuration
@Slf4j
public class PnPaperChannelMiddlewareConfigs {

    @Qualifier("pnPaperChannelConfig")
    @Autowired
    private PnPaperChannelConfig pnPaperChannelConfig;


    @Bean
    public DeliveryPushMomProducer deliveryMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new DeliveryPushMomProducer(sqsClient, this.pnPaperChannelConfig.getQueueDeliveryPush(), objMapper, DeliveryPushEvent.class);
    }

    @Bean
    public InternalQueueMomProducer internalQueueMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new InternalQueueMomProducer(sqsClient, this.pnPaperChannelConfig.getQueueInternal(), objMapper, InternalPushEvent.class);
    }
}

