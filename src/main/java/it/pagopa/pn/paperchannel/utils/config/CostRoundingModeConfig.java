package it.pagopa.pn.paperchannel.utils.config;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import lombok.Getter;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * This component handles the configuration for the CostRoundingModeConfig rounding mode.
 * It provides methods to parsing the configuration string and providing the right RoundingMode enum.
 */
@Component
public class CostRoundingModeConfig {
    private final PnPaperChannelConfig config;
    private final Set<RoundingMode> allowedValues;
    @Getter
    private RoundingMode roundingMode;


    public CostRoundingModeConfig(PnPaperChannelConfig config) {
        this.config = config;
        this.allowedValues = new HashSet<>(Arrays.asList(RoundingMode.HALF_UP, RoundingMode.HALF_DOWN));
    }

    @PostConstruct
    public void setUp() {
        RoundingMode cRMode = RoundingMode.valueOf(config.getCostRoundingMode());
        roundingMode = allowedValues.contains(cRMode)
                ?
                    cRMode
                :
                    RoundingMode.HALF_UP;
    }
}