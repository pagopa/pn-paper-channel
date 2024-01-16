package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.ApiClient;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.api.F24ControllerApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class F24ClientConfig extends CommonBaseClient {

    @Bean
    public F24ControllerApi getF24ControllerAPI(PnPaperChannelConfig pnPaperChannelConfig){

        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnPaperChannelConfig.getClientF24Basepath());
        return new F24ControllerApi(newApiClient);
    }

    @Autowired
    @Override
    public void setReadTimeoutMillis(@Value("${pn.paper-channel.f24client.timeout-millis}") int readTimeoutMillis) {
        super.setReadTimeoutMillis(readTimeoutMillis);
    }
}
