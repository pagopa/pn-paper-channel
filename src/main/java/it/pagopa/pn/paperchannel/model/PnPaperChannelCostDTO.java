package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.utils.Const.*;


@Data
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


    public BigDecimal getBasePriceFromProductType(String productType) {
        return switch (productType) {
            case RACCOMANDATA_AR, RACCOMANDATA_RIR -> basePriceAR;
            case RACCOMANDATA_SEMPLICE, RACCOMANDATA_RIS -> basePriceRS;
            case RACCOMANDATA_890 -> basePrice890;
            default -> throw new PnGenericException(INVALID_PRODUCT_TYPE, INVALID_PRODUCT_TYPE.getMessage());
        };
    }

    public BigDecimal getBasePriceForWeight(int totPagesWeight) {
        if (rangedCosts == null || rangedCosts.isEmpty())
            throw new PnGenericException(COST_NOT_FOUND, COST_NOT_FOUND.getMessage());

        return rangedCosts.stream()
                .filter(entry -> totPagesWeight >= entry.getMinWeight() && totPagesWeight <= entry.getMaxWeight())
                .findFirst()
                .map(PnPaperChannelRangeDTO::getCost)
                .orElseThrow(() -> new PnGenericException(COST_OUF_OF_RANGE, String.format("Weight %s exceeded 2000 gr", totPagesWeight)));
    }
}