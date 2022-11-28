package it.pagopa.pn.paperchannel.middleware.msclient.common;


import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "microservice")
@Import(SharedAutoConfiguration.class)
public class PnMicroservicesConfig {

    private Urls urls;
    private Extras extras;


    @Getter
    @Setter
    @ToString
    public static class Urls{
        private String safeStorage;
    }

    @Getter
    @Setter
    @ToString
    public static class Extras {
        private String safeStorageCxId;
    }

}
