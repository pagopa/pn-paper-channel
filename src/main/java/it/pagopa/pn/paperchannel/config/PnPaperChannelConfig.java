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
    private String clientDataVaultBasepath;
    private String clientAddressManagerBasepath;
    private String addressManagerCxId;
    private String addressManagerApiKey;
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
    private Long ttlPrepare;
    private Long ttlExecutionN_890;
    private Long ttlExecutionN_AR;
    private Long ttlExecutionN_RS;
    private Long ttlExecutionI_AR;
    private Long ttlExecutionI_RS;
    private Long ttlExecutionDaysMeta;
    private Long ttlExecutionDaysDemat;
    private Integer paperWeight;
    private Integer letterWeight;
    private String retryStatus;
}
