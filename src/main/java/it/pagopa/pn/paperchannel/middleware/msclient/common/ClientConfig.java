package it.pagopa.pn.paperchannel.middleware.msclient.common;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.ApiClient;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.middleware.msclient.impl.SafeStorageClientImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig extends BaseClient{

    @Bean
    public SafeStorageClient getSafeStorageClient (PnPaperChannelConfig pnPaperChannelConfig){

        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientSafeStorageBasepath());

        return new SafeStorageClientImpl(newApiClient, pnPaperChannelConfig);
    }
}
