package it.pagopa.pn.paperchannel.utils.costutils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CostWithDriver {

    private BigDecimal cost;
    private String driverCode;
    private String tenderCode;
}
