package it.pagopa.pn.paperchannel.model;

import lombok.Data;
import java.math.BigDecimal;


@Data
public class PnPaperChannelRangeDTO {
    private BigDecimal cost;
    private Integer minWeight;
    private Integer maxWeight;
}