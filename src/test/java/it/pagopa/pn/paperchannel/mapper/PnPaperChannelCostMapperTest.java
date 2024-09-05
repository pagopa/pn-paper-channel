package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelTender;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRange;
import it.pagopa.pn.paperchannel.model.PnPaperChannelCostDTO;
import it.pagopa.pn.paperchannel.model.PnPaperChannelRangeDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PnPaperChannelCostMapperTest {

    @Test
    void testToDTO() {
        // Arrange
        PnPaperChannelTender tender = new PnPaperChannelTender();
        tender.setVat(22);
        tender.setNonDeductibleVat(10);
        tender.setPagePrice(BigDecimal.valueOf(0.05));
        tender.setBasePriceAR(BigDecimal.valueOf(2.0));
        tender.setBasePriceRS(BigDecimal.valueOf(1.5));
        tender.setBasePrice890(BigDecimal.valueOf(3.0));
        tender.setFee(BigDecimal.valueOf(1.0));

        PnPaperChannelCost cost = new PnPaperChannelCost("TENDER123", "Product1", "Lot1", "Zone1");
        cost.setDeliveryDriverName("DriverA");
        cost.setDeliveryDriverId("DriverID123");
        cost.setDematerializationCost(BigDecimal.valueOf(0.10));
        cost.setCreatedAt(Instant.now());

        PnRange range1 = new PnRange();
        range1.setCost(BigDecimal.valueOf(5.0));
        range1.setMinWeight(0);
        range1.setMaxWeight(50);

        PnRange range2 = new PnRange();
        range2.setCost(BigDecimal.valueOf(7.0));
        range2.setMinWeight(51);
        range2.setMaxWeight(100);

        cost.setRangedCosts(List.of(range1, range2));

        // Act
        PnPaperChannelCostDTO dto = PnPaperChannelCostMapper.toDTO(tender, cost);

        // Assert
        assertNotNull(dto);
        assertEquals(cost.getTenderId(), dto.getTenderId());
        assertEquals(cost.getProductLotZone(), dto.getProductLotZone());
        assertEquals(cost.getProduct(), dto.getProduct());
        assertEquals(cost.getLot(), dto.getLot());
        assertEquals(cost.getZone(), dto.getZone());
        assertEquals(cost.getDeliveryDriverName(), dto.getDeliveryDriverName());
        assertEquals(cost.getDeliveryDriverId(), dto.getDeliveryDriverId());
        assertEquals(cost.getDematerializationCost(), dto.getDematerializationCost());
        assertEquals(tender.getVat(), dto.getVat());
        assertEquals(tender.getNonDeductibleVat(), dto.getNonDeductibleVat());
        assertEquals(tender.getPagePrice(), dto.getPagePrice());
        assertEquals(tender.getBasePriceAR(), dto.getBasePriceAR());
        assertEquals(tender.getBasePriceRS(), dto.getBasePriceRS());
        assertEquals(tender.getBasePrice890(), dto.getBasePrice890());
        assertEquals(tender.getFee(), dto.getFee());

        assertNotNull(dto.getRangedCosts());
        assertEquals(2, dto.getRangedCosts().size());

        PnPaperChannelRangeDTO rangeDto1 = dto.getRangedCosts().get(0);
        assertEquals(range1.getCost(), rangeDto1.getCost());
        assertEquals(range1.getMinWeight(), rangeDto1.getMinWeight());
        assertEquals(range1.getMaxWeight(), rangeDto1.getMaxWeight());

        PnPaperChannelRangeDTO rangeDto2 = dto.getRangedCosts().get(1);
        assertEquals(range2.getCost(), rangeDto2.getCost());
        assertEquals(range2.getMinWeight(), rangeDto2.getMinWeight());
        assertEquals(range2.getMaxWeight(), rangeDto2.getMaxWeight());

        assertNotNull(dto.getCreatedAt());
    }
}
