package it.pagopa.pn.paperchannel.utils;

import java.time.Instant;


public record DateChargeCalculationMode(Instant startConfigurationTime,
                                        ChargeCalculationModeEnum calculationMode) {}
