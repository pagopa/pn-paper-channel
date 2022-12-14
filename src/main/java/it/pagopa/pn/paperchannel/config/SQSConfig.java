package it.pagopa.pn.paperchannel.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.queue.action.DeliveryMomProducer;
import it.pagopa.pn.paperchannel.queue.model.DeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

//@Configuration
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
    @Bean
    public DeliveryMomProducer deliveryMomProducer(SqsClient sqsClient, ObjectMapper objectMapper){
       // return new DeliveryMomProducer(sqsClient,this.pnPaperChannelConfig.getQueueDeliveryPush(),objectMapper, DeliveryEvent.class);
        return null;
    }

    @Bean
    public AmazonSQSAsync amazonSQS() {
//        if (StringUtils.hasText(awsConfigs.getEndpointUrl())) {
//            log.info("with endpoint");
//            return AmazonSQSAsyncClientBuilder.standard()
//                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
//                    .build();
//        } else {
//            return AmazonSQSAsyncClientBuilder.standard()
//                    .withRegion(awsConfigs.getRegionCode())
//                    .build();
//        }
        return null;
    }
}