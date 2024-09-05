package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


class PnPaperChannelCostTest {
    private String tenderId;
    private String product;
    private String lot;
    private String zone;
    private String deliveryDriverName;
    private String deliveryDriverId;
    private BigDecimal dematerializationCost;
    private List<PnRange> rangedCosts;
    private Instant createdAt;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void getProductLotZoneTest() {
        //ARRANGE
        PnPaperChannelCost channelCost = initPaperChannelCost();

        // ACT
        String productLotZone = channelCost.getProductLotZone();

        //ASSERT
        Assertions.assertNotNull(channelCost);
        Assertions.assertEquals(String.join("#", product, lot, zone), productLotZone);
    }

    private PnPaperChannelCost initPaperChannelCost() {
        PnPaperChannelCost channelCost = new PnPaperChannelCost(tenderId, product, lot, zone);
        channelCost.setDeliveryDriverName(deliveryDriverName);
        channelCost.setDeliveryDriverId(deliveryDriverId);
        channelCost.setDematerializationCost(dematerializationCost);
        channelCost.setRangedCosts(rangedCosts);
        channelCost.setCreatedAt(createdAt);
        return channelCost;
    }

    private void initialize() {
        tenderId = "tenderId";
        product = "890";
        lot = "A";
        zone = "ZONE_1";
        deliveryDriverName = "deliveryDriverName";
        deliveryDriverId = "deliveryDriverId";
        dematerializationCost = BigDecimal.valueOf(1245);
        rangedCosts = new ArrayList<>();
        createdAt = Instant.now();
    }
}