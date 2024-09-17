package it.pagopa.pn.paperchannel.utils.config;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.math.RoundingMode;


/**
 * This component handles the configuration for the CostRoundingModeConfig rounding mode.
 * It provides methods to parsing the configuration string and providing the right RoundingMode enum.
 */
@Component
@RequiredArgsConstructor
public class CostRoundingModeConfig {
    private final PnPaperChannelConfig config;
    @Getter
    private RoundingMode roundingMode;


    @PostConstruct
    public void setUp() {
        roundingMode = RoundingMode.valueOf(config.getCostRoundingMode());
    }
}