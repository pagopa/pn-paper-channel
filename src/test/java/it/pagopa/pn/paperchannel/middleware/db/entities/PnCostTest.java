package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;


class PnCostTest {
    private PnCost pnCost;
    private PnCost toPnCost;
    private String deliveryDriverCode;
    private String uuid;
    private List<String> cap;
    private String zone;
    private String tenderCode;
    private String productType;
    private BigDecimal basePrice;
    private BigDecimal pagePrice;
    private Boolean fsu;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void isEqualsTest() {
        pnCost = initCost();
        boolean isEquals = pnCost.equals(toPnCost);
        Assertions.assertFalse(isEquals);

        isEquals = pnCost.equals(pnCost);
        Assertions.assertTrue(isEquals);

        PnDeliveryDriver fakePnCost = new PnDeliveryDriver();
        isEquals = pnCost.equals(fakePnCost);
        Assertions.assertFalse(isEquals);

        toPnCost = initCost();
        toPnCost.setFsu(false);
        toPnCost.setCap(null);
        isEquals = pnCost.equals(toPnCost);
        Assertions.assertFalse(isEquals);

        toPnCost = initCost();
        toPnCost.setFsu(false);
        toPnCost.setZone("ZONE_2");
        isEquals = pnCost.equals(toPnCost);
        Assertions.assertFalse(isEquals);

        toPnCost = initCost();
        toPnCost.setFsu(false);
        toPnCost.setTenderCode("ZAXSCDVFBGNK");
        isEquals = pnCost.equals(toPnCost);
        Assertions.assertFalse(isEquals);

        toPnCost = initCost();
        toPnCost.setFsu(false);
        toPnCost.setProductType("AR");
        isEquals = pnCost.equals(toPnCost);
        Assertions.assertFalse(isEquals);
    }

    @Test
    void toStringTest() {
        pnCost = initCost();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnCost.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("deliveryDriverCode=");
        stringBuilder.append(deliveryDriverCode);
        stringBuilder.append(", ");
        stringBuilder.append("uuid=");
        stringBuilder.append(uuid);
        stringBuilder.append(", ");
        stringBuilder.append("tenderCode=");
        stringBuilder.append(tenderCode);
        stringBuilder.append(", ");
        stringBuilder.append("productType=");
        stringBuilder.append(productType);
        stringBuilder.append(", ");
        stringBuilder.append("basePrice=");
        stringBuilder.append(basePrice);
        stringBuilder.append(", ");
        stringBuilder.append("pagePrice=");
        stringBuilder.append(pagePrice);
        stringBuilder.append(", ");
        stringBuilder.append("fsu=");
        stringBuilder.append(fsu);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnCost.toString());
    }

    @Test
    void hashCodeTest() {
        PnCost pnCostA = initCost();
        PnCost pnCostB = initCost();
        Assertions.assertTrue(pnCostA.equals(pnCostB) && pnCostB.equals(pnCostA));
        Assertions.assertEquals(pnCostA.hashCode(), pnCostB.hashCode());
    }

    private PnCost initCost() {
        PnCost pnCost = new PnCost();
        pnCost.setUuid(uuid);
        pnCost.setCap(cap);
        pnCost.setFsu(true);
        pnCost.setBasePrice(basePrice);
        pnCost.setTenderCode(tenderCode);
        pnCost.setDeliveryDriverCode(deliveryDriverCode);
        pnCost.setPagePrice(pagePrice);
        pnCost.setProductType(productType);
        pnCost.setZone(zone);
        return pnCost;
    }

    private void initialize() {
        deliveryDriverCode = "LOKIJUHYGTFR";
        uuid = "5432106789";
        cap = Arrays.asList("00100", "09681", "12031", "01020");
        zone = "ZONE_1";
        tenderCode = "ZAXSCDVFBGNH";
        productType = "890";
        basePrice = BigDecimal.valueOf(12.3F);
        pagePrice = BigDecimal.valueOf(1.8F);
        fsu = true;;
    }
}
