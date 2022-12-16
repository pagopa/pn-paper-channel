package it.pagopa.pn.paperchannel.middleware;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.queue.action.DeliveryMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@Slf4j
public class PnPaperChannelMiddlewareConfigs {

    private final PnPaperChannelConfig pnPaperChannelConfig;

    public PnPaperChannelMiddlewareConfigs(PnPaperChannelConfig cfg) {
        this.pnPaperChannelConfig = cfg;
    }

    @Bean
    public DeliveryMomProducer deliveryMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        log.info("try to start queue ...");
        log.info("queue name url "+pnPaperChannelConfig.getQueueDeliveryPush());
        if (sqsClient == null ) {
            log.info(" sqsClient is null");
        } else {
            log.info("sqsClient is not null");
        }
      
        return new DeliveryMomProducer(sqsClient, this.pnPaperChannelConfig.getQueueDeliveryPush(), objMapper, DeliveryEvent.class);
    }
}

