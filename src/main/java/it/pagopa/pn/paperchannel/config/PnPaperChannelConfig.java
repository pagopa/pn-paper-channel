package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "pn.paper-channel")
@Import(SharedAutoConfiguration.class)
@Slf4j
public class PnPaperChannelConfig {

    private String clientSafeStorageBasepath;
    private String clientNationalRegistriesBasepath;
    private String clientExternalChannelBasepath;
    private String clientF24Basepath;
    private String clientDataVaultBasepath;
    private String clientAddressManagerBasepath;
    private String addressManagerCxId;
    private String addressManagerApiKey;
    private String safeStorageCxId;
    private String f24CxId;
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
    private Integer attemptQueueF24;
    private Integer attemptQueueZipHandle;
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
    private List<String> dateChargeCalculationModes;
    private Duration refinementDuration;
    private String requestPaIdOverride;
    private Set<String> requiredDemats;
    private boolean zipHandleActive;

    /**
     * Per l'errore PNADDR001: True se il failureDetailCode D01 deve essere mandato a delivery push (specificando anche l'indirizzo),
     * false se invece viene salvato l'errore sulla tabella degli errori (as-is)
     */
    private boolean pnaddr001sendD01ToDeliveryPush;

    /**
     * Per l'errore PNADDR002: True se il failureDetailCode D01 deve essere mandato a delivery push (specificando anche l'indirizzo),
     * false se invece viene salvato l'errore sulla tabella degli errori (as-is)
     */
    private boolean pnaddr002sendD01ToDeliveryPush;

    @PostConstruct
    public void init() {
        log.info("CONFIGURATIONS: {}", this);
    }


    public Duration getRefinementDuration() {
        if (this.refinementDuration == null)
        {
            this.refinementDuration = Duration.of(10, ChronoUnit.DAYS);
        }

        return this.refinementDuration;
    }
}
