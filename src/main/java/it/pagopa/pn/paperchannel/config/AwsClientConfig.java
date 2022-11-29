package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.configs.RuntimeMode;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.configs.aws.AwsServicesClientsConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsClientConfig extends AwsServicesClientsConfig {

    public AwsClientConfig(AwsConfigs props) {
        super(props, RuntimeMode.DEVELOPMENT);
    }

}
