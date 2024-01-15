package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnChargeCalculationModeNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;


class DateChargeCalculationModesUtilsTest {


    @Test
    void getChargeCalculationModeAARTest() {
        PnPaperChannelConfig config = new PnPaperChannelConfig();
        config.setDateChargeCalculationModes(List.of(
                "1970-01-01T00:00:00Z;AAR",
                "2100-01-01T00:00:00Z;COMPLETE"
        ));
        DateChargeCalculationModesUtils dateChargeCalculationModesUtils = new DateChargeCalculationModesUtils(config);

        assertThat(dateChargeCalculationModesUtils.getChargeCalculationMode()).isEqualTo(ChargeCalculationModeEnum.AAR);
    }

    @Test
    void getChargeCalculationModeCOMPLETETest() {
        PnPaperChannelConfig config = new PnPaperChannelConfig();
        config.setDateChargeCalculationModes(List.of(
                "1970-01-01T00:00:00Z;AAR",
                "1990-01-01T00:00:00Z;COMPLETE"
        ));
        DateChargeCalculationModesUtils dateChargeCalculationModesUtils = new DateChargeCalculationModesUtils(config);

        assertThat(dateChargeCalculationModesUtils.getChargeCalculationMode()).isEqualTo(ChargeCalculationModeEnum.COMPLETE);
    }

    @Test
    void validateOKTest() {
        PnPaperChannelConfig config = new PnPaperChannelConfig();
        config.setDateChargeCalculationModes(List.of(
                "1970-01-01T00:00:00Z;AAR",
                "1990-01-01T00:00:00Z;COMPLETE"
        ));
        DateChargeCalculationModesUtils dateChargeCalculationModesUtils = new DateChargeCalculationModesUtils(config);

        assertThatNoException().isThrownBy(dateChargeCalculationModesUtils::validate);
    }

    @Test
    void validateKOBecauseNoDateFoundTest() {
        PnPaperChannelConfig config = new PnPaperChannelConfig();
        config.setDateChargeCalculationModes(List.of(
                "2100-01-01T00:00:00Z;AAR",
                "2110-01-01T00:00:00Z;COMPLETE"
        ));
        DateChargeCalculationModesUtils dateChargeCalculationModesUtils = new DateChargeCalculationModesUtils(config);

        assertThatExceptionOfType(PnChargeCalculationModeNotFoundException.class).isThrownBy(dateChargeCalculationModesUtils::validate);
    }

    @Test
    void constructorKOBecauseErrorEnumTest() {
        PnPaperChannelConfig config = new PnPaperChannelConfig();
        config.setDateChargeCalculationModes(List.of(
                "1970-01-01T00:00:00Z;AAR_MISTAKE",
                "1990-01-01T00:00:00Z;COMPLETE"
        ));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new DateChargeCalculationModesUtils(config));
    }

    @Test
    void constructorKOBecauseErrorDateFormatTest() {
        PnPaperChannelConfig config = new PnPaperChannelConfig();
        config.setDateChargeCalculationModes(List.of(
                "1970-01-01T00:00:00;AAR",
                "1990-01-01T00:00:00Z;COMPLETE"
        ));

        assertThatExceptionOfType(DateTimeParseException.class).isThrownBy(() -> new DateChargeCalculationModesUtils(config));
    }

}
