package it.pagopa.pn.paperchannel.dao.model;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class DeliveryAndCostTest {



    @Test
    void checkEqualsTest(){
        DeliveryAndCost cost1 = new DeliveryAndCost();
        cost1.setTaxId("1234");
        cost1.setUniqueCode("12222");
        cost1.setZone("ZONE_1");
        cost1.setProductType("AR");
        cost1.setFsu(true);
        DeliveryAndCost cost2 = new DeliveryAndCost();
        cost2.setTaxId(cost1.getTaxId());
        cost2.setUniqueCode(cost1.getUniqueCode());
        cost2.setZone(cost1.getZone());
        cost2.setProductType("AR");
        cost2.setFsu(true);

        Assertions.assertEquals(cost1, cost2);

        cost2.setZone("ZONE_2");
        Assertions.assertNotEquals(cost1, cost2);

        cost2.setCaps(List.of("12222"));
        cost2.setZone(null);
        Assertions.assertNotEquals(cost1, cost2);

        cost1.setCaps(List.of("12222"));
        cost1.setZone(null);
        Assertions.assertEquals(cost1, cost2);
    }

    @Test
    void hashCodeTest(){
        DeliveryAndCost cost1 = new DeliveryAndCost();
        cost1.setTaxId("1234");
        cost1.setUniqueCode("12222");
        cost1.setZone("ZONE_1");
        cost1.setProductType("AR");
        cost1.setFsu(true);
        DeliveryAndCost cost2 = new DeliveryAndCost();
        cost2.setTaxId(cost1.getTaxId());
        cost2.setUniqueCode(cost1.getUniqueCode());
        cost2.setZone(cost1.getZone());
        cost2.setProductType("AR");
        cost2.setFsu(true);

        Assertions.assertEquals(cost1.hashCode(), cost2.hashCode());

        cost2.setCaps(List.of("12222"));
        cost2.setZone(null);
        Assertions.assertNotEquals(cost1, cost2);
    }



}
