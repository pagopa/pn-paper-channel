package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelTender;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRange;
import it.pagopa.pn.paperchannel.model.PnPaperChannelCostDTO;
import it.pagopa.pn.paperchannel.model.PnPaperChannelRangeDTO;

public class PnPaperChannelCostMapper {

    private PnPaperChannelCostMapper() {}

    public static PnPaperChannelCostDTO toDTO(PnPaperChannelTender tender, PnPaperChannelCost cost) {
        var dto = new PnPaperChannelCostDTO();
        dto.setTenderId(cost.getTenderId());
        dto.setProductLotZone(cost.getProductLotZone());
        dto.setProduct(cost.getProduct());
        dto.setLot(cost.getLot());
        dto.setZone(cost.getZone());
        dto.setDeliveryDriverName(cost.getDeliveryDriverName());
        dto.setDeliveryDriverId(cost.getDeliveryDriverId());
        dto.setDematerializationCost(cost.getDematerializationCost());
        dto.setVat(tender.getVat());
        dto.setNonDeductibleVat(tender.getNonDeductibleVat());
        dto.setPagePrice(tender.getPagePrice());
        dto.setBasePriceAR(tender.getBasePriceAR());
        dto.setBasePriceRS(tender.getBasePriceRS());
        dto.setBasePrice890(tender.getBasePrice890());
        dto.setFee(tender.getFee());
        if (cost.getRangedCosts() != null) {
            dto.setRangedCosts(
                    cost.getRangedCosts().stream().map(PnPaperChannelCostMapper::toRangeDTO).toList()
            );
        }
        dto.setCreatedAt(cost.getCreatedAt());
        return dto;
    }

    private static PnPaperChannelRangeDTO toRangeDTO(PnRange range) {
        var dto = new PnPaperChannelRangeDTO();
        dto.setCost(range.getCost());
        dto.setMinWeight(range.getMinWeight());
        dto.setMaxWeight(range.getMaxWeight());
        return dto;
    }
}
