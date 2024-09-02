package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@Getter
@Setter
public class PnPaperChannelCostDTO {
    private String tenderId;
    private String productLotZone;
    private String product;
    private String lot;
    private String zone;
    private String deliveryDriverName;
    private String deliveryDriverId;
    private BigDecimal dematerializationCost;
    private Integer vat;
    private Integer nonDeductibleVat;
    private BigDecimal pagePrice;
    private BigDecimal basePriceAR;
    private BigDecimal basePriceRS;
    private BigDecimal basePrice890;
    private BigDecimal fee;
    private List<PnPaperChannelRangeDTO> rangedCosts;
    private Instant createdAt;
}
