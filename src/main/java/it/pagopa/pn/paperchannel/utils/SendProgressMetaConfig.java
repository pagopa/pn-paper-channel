package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

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
        DISABLED,
        META,
        RECAG012A,
        CON018;
    }

    private Set<SendProgressMetaEnum> enabledFlags;

    /**
     * Validates the SendProgressMeta configuration and initializes the enabled flags.
     * This method is automatically called after dependency injection.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    @PostConstruct
    public void validateConfiguration() {
        List<String> configValues = config.getSendProgressMeta();
        if (configValues == null || configValues.isEmpty()) {
            enabledFlags = Collections.emptySet();
            return;
        }

        Set<SendProgressMetaEnum> parsedFlags = new HashSet<>();
        boolean hasDisable = false;

        for (String value : configValues) {
            try {
                SendProgressMetaEnum flag = SendProgressMetaEnum.valueOf(value);
                parsedFlags.add(flag);
                if (flag == SendProgressMetaEnum.DISABLED) {
                    hasDisable = true;
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid value in SendProgressMeta config: {}", value);
                throw new IllegalStateException("Invalid value in SendProgressMeta config: " + value, e);
            }
        }

        if (hasDisable && parsedFlags.size() > 1) {
            log.error("SendProgressMeta config contains DISABLE along with other values");
            throw new IllegalStateException("SendProgressMeta config contains DISABLE along with other values");
        }

        enabledFlags = hasDisable ? Collections.emptySet() : EnumSet.copyOf(parsedFlags);
    }


    /**
     * Checks if the META flag is enabled.
     *
     * @return true if META is enabled, false otherwise
     */
    public boolean isMetaEnabled() {
        return enabledFlags.contains(SendProgressMetaEnum.META);
    }

    /**
     * Checks if the RECAG012A flag is enabled.
     *
     * @return true if RECAG012A is enabled, false otherwise
     */
    public boolean isRECAG012AEnabled() {
        return enabledFlags.contains(SendProgressMetaEnum.RECAG012A);
    }

    /**
     * Checks if the CON018 flag is enabled.
     *
     * @return true if CON018 is enabled, false otherwise
     */
    public boolean isCON018Enabled() {
        return enabledFlags.contains(SendProgressMetaEnum.CON018);
    }

    /**
     * Checks if the feature is disabled.
     *
     * @return true if the feature is disabled, false otherwise
     */
    public boolean isDisabled() {
        return enabledFlags.isEmpty();
    }
}
