package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.model.Range;
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
    private List<Range> rangedCosts;
    private Instant createdAt;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnPaperChannelCost channelCost = initPaperChannelCost();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(channelCost.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("tenderId=");
        stringBuilder.append(tenderId);
        stringBuilder.append(", ");
        stringBuilder.append("product=");
        stringBuilder.append(product);
        stringBuilder.append(", ");
        stringBuilder.append("lot=");
        stringBuilder.append(lot);
        stringBuilder.append(", ");
        stringBuilder.append("zone=");
        stringBuilder.append(zone);
        stringBuilder.append(", ");
        stringBuilder.append("deliveryDriverName=");
        stringBuilder.append(deliveryDriverName);
        stringBuilder.append(", ");
        stringBuilder.append("deliveryDriverId=");
        stringBuilder.append(deliveryDriverId);
        stringBuilder.append(", ");
        stringBuilder.append("dematerializationCost=");
        stringBuilder.append(dematerializationCost);
        stringBuilder.append(", ");
        stringBuilder.append("rangedCosts=");
        stringBuilder.append(rangedCosts);
        stringBuilder.append(", ");
        stringBuilder.append("createdAt=");
        stringBuilder.append(createdAt);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, channelCost.toString());
    }

    @Test
    void getProductLotZoneTest() {
        PnPaperChannelCost channelCost = initPaperChannelCost();
        Assertions.assertNotNull(channelCost);
        Assertions.assertTrue(channelCost.getProductLotZone().contains(product));
        Assertions.assertTrue(channelCost.getProductLotZone().contains(lot));
        Assertions.assertTrue(channelCost.getProductLotZone().contains(zone));
    }

    private PnPaperChannelCost initPaperChannelCost() {
        PnPaperChannelCost channelCost = new PnPaperChannelCost();
        channelCost.setTenderId(tenderId);
        channelCost.setProduct(product);
        channelCost.setLot(lot);
        channelCost.setZone(zone);
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