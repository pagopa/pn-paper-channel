package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * This component handles the configuration for the SendProgressMeta feature flag.
 * It provides methods to check if specific feature flags are enabled or disabled.
 * It also validates the configuration to ensure only allowed values are present.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SendProgressMetaConfig {
    private final PnPaperChannelConfig config;

    /**
     * Enumeration of possible values for the SendProgressMeta feature flag.
     */
    public enum SendProgressMetaEnum {
        DISABLE,
        META,
        RECAG012,
        CON018;
    }

    /**
     * Validates the SendProgressMeta configuration.
     * Ensures that only allowed values are present and if DISABLE is present, it is the only value.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    @PostConstruct
    public void validateConfiguration() {
        List<String> configValues = config.getSendProgressMeta();
        if (configValues != null && !configValues.isEmpty()) {
            boolean hasDisable = configValues.contains(SendProgressMetaEnum.DISABLE.name());
            for (String value : configValues) {
                try {
                    SendProgressMetaEnum.valueOf(value);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Invalid value in SendProgressMeta config: " + value, e);
                }
            }
            if (hasDisable && configValues.size() > 1) {
                throw new IllegalStateException("SendProgressMeta config contains DISABLE along with other values");
            }
        }
    }

    /**
     * Retrieves the list of enabled flags from the configuration.
     *
     * @return a list of enabled flags
     */
    private List<String> getEnabledFlags() {
        List<String> configValues = config.getSendProgressMeta();
        if (configValues == null ||
                configValues.isEmpty() ||
                configValues.contains(SendProgressMetaEnum.DISABLE.name())) {
            return List.of();
        }
        return configValues;
    }

    /**
     * Checks if the META flag is enabled.
     *
     * @return true if META is enabled, false otherwise
     */
    public boolean isMetaEnabled() {
        return getEnabledFlags().contains(SendProgressMetaEnum.META.name());
    }

    /**
     * Checks if the RECAG012 flag is enabled.
     *
     * @return true if RECAG012 is enabled, false otherwise
     */
    public boolean isRECAG012Enabled() {
        return getEnabledFlags().contains(SendProgressMetaEnum.RECAG012.name());
    }

    /**
     * Checks if the CON018 flag is enabled.
     *
     * @return true if CON018 is enabled, false otherwise
     */
    public boolean isCCON018Enabled() {
        return getEnabledFlags().contains(SendProgressMetaEnum.CON018.name());
    }

    /**
     * Checks if the feature is disabled.
     *
     * @return true if the feature is disabled, false otherwise
     */
    public boolean isDisabled() {
        return getEnabledFlags().isEmpty();
    }
}
