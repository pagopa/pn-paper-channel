package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnChargeCalculationModeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
public class DateChargeCalculationModesUtils {

    private static final String SEPARATOR = ";";
    private static final int START_DATE_INDEX = 0;
    private static final int CALCULATION_MODE_INDEX = 1;

    private final List<DateChargeCalculationMode> dateChargeCalculationModes;


    public DateChargeCalculationModesUtils(PnPaperChannelConfig config) {
        dateChargeCalculationModes = buildPaperSendModeFromString(config.getDateChargeCalculationModes());
    }

    private List<DateChargeCalculationMode> buildPaperSendModeFromString(List<String> chargeCalculationModesString) {

        return chargeCalculationModesString.stream()
                .map(this::toChargeCalculationMode)
                .sorted(Comparator.comparing(DateChargeCalculationMode::startConfigurationTime).reversed())
                .toList();

    }

    /**
     *
     * @param chargeCalculationModeString in startConfigurationTime;calculationMode format. Example:
     *                                    1970-01-01T00:00:00Z;AAR,2024-01-31T23:00:00Z;COMPLETE
     * @return the {@link ChargeCalculationModeEnum} configured for the date of "now"
     */
    private DateChargeCalculationMode toChargeCalculationMode(String chargeCalculationModeString) {
        String[] chargeCalculationModeSplit = chargeCalculationModeString.split(SEPARATOR);
        Instant startDate = Instant.parse(chargeCalculationModeSplit[START_DATE_INDEX]);
        ChargeCalculationModeEnum calculationModeEnum = ChargeCalculationModeEnum.valueOf(chargeCalculationModeSplit[CALCULATION_MODE_INDEX]);
        return new DateChargeCalculationMode(startDate, calculationModeEnum);
    }

    public ChargeCalculationModeEnum getChargeCalculationMode() {
        Instant now = Instant.now();
        return dateChargeCalculationModes.stream()
                .filter(dateChargeCalculationMode -> now.toEpochMilli() >= dateChargeCalculationMode.startConfigurationTime().toEpochMilli())
                .findFirst()
                .map(DateChargeCalculationMode::calculationMode)
                .orElseThrow(() -> new PnChargeCalculationModeNotFoundException(now));
    }

    @PostConstruct
    public void validate() {
        log.debug("Validating chargeCalculationModes...");
        getChargeCalculationMode();
        log.debug("Validated chargeCalculationModes");
    }



}
