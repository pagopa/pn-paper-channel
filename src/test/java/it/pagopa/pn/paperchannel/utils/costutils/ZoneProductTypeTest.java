package it.pagopa.pn.paperchannel.utils.costutils;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZoneProductTypeTest {
    private ZoneProductType zoneProductType;
    private ZoneProductType toZoneProductType;
    private String zone;
    private String productType;
    private boolean fsu;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void setGetTest() {
        zoneProductType = new ZoneProductType();
        Assertions.assertNotNull(zoneProductType);

        zoneProductType = initZoneProductType();
        Assertions.assertNotNull(zoneProductType);
        Assertions.assertEquals(zone, zoneProductType.getZone());
        Assertions.assertEquals(productType, zoneProductType.getProductType());

        String zone = "ZONE_2";
        String productType = "AR";
        boolean fsu = false;

        zoneProductType.setZone(zone);
        zoneProductType.setProductType(productType);
        zoneProductType.setFsu(fsu);

        Assertions.assertEquals(zone, zoneProductType.getZone());
        Assertions.assertEquals(productType, zoneProductType.getProductType());
    }

    @Test
    void isEqualsTest() {
        zoneProductType = initZoneProductType();
        boolean isEquals = zoneProductType.equals(toZoneProductType);
        Assertions.assertFalse(isEquals);

        isEquals = zoneProductType.equals(zoneProductType);
        Assertions.assertTrue(isEquals);

        PnDeliveryDriver fakePnCost = new PnDeliveryDriver();
        isEquals = zoneProductType.equals(fakePnCost);
        Assertions.assertFalse(isEquals);

        toZoneProductType = initZoneProductType();
        toZoneProductType.setFsu(false);
        isEquals = zoneProductType.equals(toZoneProductType);
        Assertions.assertFalse(isEquals);

        toZoneProductType = initZoneProductType();
        toZoneProductType.setFsu(false);
        toZoneProductType.setZone("ZONE_2");
        isEquals = zoneProductType.equals(toZoneProductType);
        Assertions.assertFalse(isEquals);

        toZoneProductType = initZoneProductType();
        toZoneProductType.setFsu(false);
        toZoneProductType.setProductType("AR");
        isEquals = zoneProductType.equals(toZoneProductType);
        Assertions.assertFalse(isEquals);
    }

    private ZoneProductType initZoneProductType() {
        ZoneProductType zoneProductType = new ZoneProductType();
        zoneProductType.setZone(zone);
        zoneProductType.setProductType(productType);
        zoneProductType.setFsu(fsu);
        return zoneProductType;
    }

    private void initialize() {
        zone = "ZONE_1";
        productType = "890";
        fsu = true;;
    }
}
