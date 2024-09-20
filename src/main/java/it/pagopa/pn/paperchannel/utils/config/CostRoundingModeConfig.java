package it.pagopa.pn.paperchannel.utils.config;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import lombok.Getter;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INCORRECT_ROUNDING_MODE;


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
        this.allowedValues = new HashSet<>(Arrays.asList(RoundingMode.HALF_UP, RoundingMode.UP));
    }

    @PostConstruct
    public void setUp() {
        roundingMode = RoundingMode.valueOf(config.getCostRoundingMode());
        if(!allowedValues.contains(roundingMode)) {
            throw new IllegalArgumentException(INCORRECT_ROUNDING_MODE.getMessage());
        }
    }
}