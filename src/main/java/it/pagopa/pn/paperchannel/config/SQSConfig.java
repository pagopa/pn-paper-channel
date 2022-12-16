package it.pagopa.pn.paperchannel.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
public class SQSConfig {

    private final AwsPropertiesConfig awsConfigs;
    private final PnPaperChannelConfig pnPaperChannelConfig;

    public SQSConfig(AwsPropertiesConfig awsConfigs, PnPaperChannelConfig pnPaperChannelConfig) {
        this.awsConfigs = awsConfigs;
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }

    @Bean
    public AmazonSQSAsync amazonSQS() {
        if (StringUtils.hasText(awsConfigs.getEndpointUrl())) {
            return AmazonSQSAsyncClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
                    .build();
        } else {
            return AmazonSQSAsyncClientBuilder.standard()
                    .withRegion(awsConfigs.getRegionCode())
                    .build();
        }
    }
}