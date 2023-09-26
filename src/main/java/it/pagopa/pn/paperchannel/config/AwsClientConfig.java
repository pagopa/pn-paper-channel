package it.pagopa.pn.paperchannel.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsyncClientBuilder;
import it.pagopa.pn.commons.configs.RuntimeMode;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.configs.aws.AwsServicesClientsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsClientConfig extends AwsServicesClientsConfig {

    public AwsClientConfig(AwsConfigs props) {
        super(props, RuntimeMode.PROD);
    }

    @Bean
    public AmazonEventBridgeAsync amazonEventBridgeAsync(AwsConfigs props) {
        return AmazonEventBridgeAsyncClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(props.getRegionCode())
                .build();
    }

}