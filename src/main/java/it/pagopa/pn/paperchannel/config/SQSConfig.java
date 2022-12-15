package it.pagopa.pn.paperchannel.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.middleware.queue.action.DeliveryMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@Slf4j
public class SQSConfig {

    private final AwsPropertiesConfig awsConfigs;
    private final PnPaperChannelConfig pnPaperChannelConfig;

    public SQSConfig(AwsPropertiesConfig awsConfigs, PnPaperChannelConfig pnPaperChannelConfig) {
        this.awsConfigs = awsConfigs;
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }

    /**
     * Si è reso necessario fornire un bean di AmazonSQSAsync perchè AmazonSQSBufferedAsyncClient
     * utilizzato di default dalla libreria spring-cloud-aws non supportava le code FIFO
     *
     * https://docs.awspring.io/spring-cloud-aws/docs/2.4.2/reference/html/index.html#fifo-queue-support
     * @return bean per le code
     */
//    @Bean
//    public DeliveryMomProducer deliveryMomProducer(SqsClient sqsClient, ObjectMapper objectMapper){
//        log.info("try to start queue ...");
//        log.info("queue name url "+pnPaperChannelConfig.getQueueDeliveryPush());
//        if (sqsClient == null ) {
//            log.info(" sqsClient is null");
//        } else {
//            log.info("sqsClient is not null");
//        }
//        if (sqsClient.listQueues() != null) {
//            sqsClient.listQueues().queueUrls().stream().forEach(s -> {
//                log.info("url : "+s);
//            });
//        }
//
//        return new DeliveryMomProducer(sqsClient,this.pnPaperChannelConfig.getQueueDeliveryPush(),objectMapper, DeliveryEvent.class);
//    }

    @Bean
    public AmazonSQSAsync amazonSQS() {
        log.info("init amazonSQS region" + awsConfigs.getRegionCode());

        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("https://sqs.eu-south-1.amazonaws.com", awsConfigs.getRegionCode()))
                .build();

//        if (StringUtils.hasText(awsConfigs.getEndpointUrl())) {
//            log.info("with endpoint");
//            return AmazonSQSAsyncClientBuilder.standard()
//                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
//                    .build();
//        } else {
//            log.info("with no endpoint");
//            return AmazonSQSAsyncClientBuilder.standard()
//                    .withRegion(awsConfigs.getRegionCode())
//                    .build();
//        }
    }
}