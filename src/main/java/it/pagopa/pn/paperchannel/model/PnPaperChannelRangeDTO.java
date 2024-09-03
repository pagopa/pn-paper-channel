package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
public class PnPaperChannelRangeDTO {
    private BigDecimal cost;
    private Integer minWeight;
    private Integer maxWeight;
}
