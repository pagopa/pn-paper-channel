package it.pagopa.pn.paperchannel.config;

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
@ConfigurationProperties(prefix = "pn.paper-channel")
@Import(SharedAutoConfiguration.class)
public class PnPaperChannelConfig {

    private String clientSafeStorageBasepath;
    private String clientNationalRegistriesBasepath;
    private String clientExternalChannelBasepath;
    private String safeStorageCxId;
    private String xPagopaExtchCxId;
    private String nationalRegistryCxId;
    private String queueDeliveryPush;
    private String queueNationalRegistries;
    private String queueExternalChannel;
    private String queueInternal;
    private Integer attemptSafeStorage;
    private Integer attemptQueueSafeStorage;
    private Integer attemptQueueExternalChannel;
    private Integer attemptQueueNationalRegistries;


}
