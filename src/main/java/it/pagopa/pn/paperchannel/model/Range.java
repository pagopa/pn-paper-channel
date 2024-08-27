package it.pagopa.pn.paperchannel.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;


@Getter
@Setter
@ToString
@AllArgsConstructor
public class Range {
    private BigDecimal cost;
    private Integer minWeight;
    private Integer maxWeight;
}
