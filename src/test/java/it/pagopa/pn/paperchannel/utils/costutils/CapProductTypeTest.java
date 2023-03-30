package it.pagopa.pn.paperchannel.utils.costutils;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapProductTypeTest {
    private CapProductType capProductType;
    private CapProductType toCapProductType;
    private String cap;
    private String productType;
    private boolean fsu;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void setGetTest() {
        capProductType = new CapProductType();
        Assertions.assertNotNull(capProductType);

        capProductType = initCapProductType();
        Assertions.assertNotNull(capProductType);
        Assertions.assertEquals(cap, capProductType.getCap());
        Assertions.assertEquals(productType, capProductType.getProductType());

        String cap = "09610";
        String productType = "AR";
        boolean fsu = false;

        capProductType.setCap(cap);
        capProductType.setProductType(productType);
        capProductType.setFsu(fsu);

        Assertions.assertEquals(cap, capProductType.getCap());
        Assertions.assertEquals(productType, capProductType.getProductType());
    }

    @Test
    void isEqualsTest() {
        capProductType = initCapProductType();
        boolean isEquals = capProductType.equals(toCapProductType);
        Assertions.assertFalse(isEquals);

        isEquals = capProductType.equals(capProductType);
        Assertions.assertTrue(isEquals);

        PnDeliveryDriver fakePnCost = new PnDeliveryDriver();
        isEquals = capProductType.equals(fakePnCost);
        Assertions.assertFalse(isEquals);

        toCapProductType = initCapProductType();
        toCapProductType.setFsu(false);
        toCapProductType.setCap("00789");
        isEquals = capProductType.equals(toCapProductType);
        Assertions.assertFalse(isEquals);

        toCapProductType = initCapProductType();
        toCapProductType.setFsu(false);
        toCapProductType.setProductType("AR");
        isEquals = capProductType.equals(toCapProductType);
        Assertions.assertFalse(isEquals);
    }

    private CapProductType initCapProductType() {
        CapProductType capProductType = new CapProductType();
        capProductType.setCap(cap);
        capProductType.setProductType(productType);
        capProductType.setFsu(fsu);
        return capProductType;
    }

    private void initialize() {
        cap = "00100";
        productType = "890";
        fsu = true;;
    }
}
