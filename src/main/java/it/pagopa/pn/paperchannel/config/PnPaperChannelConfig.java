package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
    private Integer attemptQueueAddressManager;
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
    private String chargeCalculationMode;
    private String originalPostmanAddressUsageMode;
    private Duration refinementDuration;
    private String requestPaIdOverride;

    public String getOriginalPostmanAddressUsageMode() {
        if (StringUtils.isBlank(originalPostmanAddressUsageMode)){
            return "PAPERSEND";
        }
        return this.originalPostmanAddressUsageMode;
    }


    public Duration getRefinementDuration() {
        if (this.refinementDuration == null)
        {
            this.refinementDuration = Duration.of(10, ChronoUnit.DAYS);
        }

        return this.refinementDuration;
    }
}
