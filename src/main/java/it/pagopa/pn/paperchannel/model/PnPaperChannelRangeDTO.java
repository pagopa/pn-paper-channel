package it.pagopa.pn.paperchannel.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Data
public class PnPaperChannelRangeDTO {
    private BigDecimal cost;
    private Integer minWeight;
    private Integer maxWeight;
}
