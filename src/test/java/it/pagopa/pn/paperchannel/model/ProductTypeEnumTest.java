package it.pagopa.pn.paperchannel.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductTypeEnumTest {

    @Test
    void valueOfTest() {
        assertEquals("RN_AR", ProductTypeEnum.RN_AR.name());
        assertEquals("RN_890", ProductTypeEnum.RN_890.name());
        assertEquals("RN_RS", ProductTypeEnum.RN_RS.name());
        assertEquals("RI_AR", ProductTypeEnum.RI_AR.name());
        assertEquals("RI_RS", ProductTypeEnum.RI_RS.name());
    }

    @Test
    void fromValueTest() {
        ProductTypeEnum productTypeEnum = ProductTypeEnum.fromValue("RN_AR");
        assertEquals("RN_AR", productTypeEnum.name());
        productTypeEnum = ProductTypeEnum.fromValue("RN_890");
        assertEquals("RN_890", productTypeEnum.name());
        productTypeEnum = ProductTypeEnum.fromValue("RN_RS");
        assertEquals("RN_RS", productTypeEnum.name());
        productTypeEnum = ProductTypeEnum.fromValue("RI_AR");
        assertEquals("RI_AR", productTypeEnum.name());
        productTypeEnum = ProductTypeEnum.fromValue("RI_RS");
        assertEquals("RI_RS", productTypeEnum.name());
    }

    @Test
    void fromValueTestThrowsError() {
        String enumValue = "XY_ZW";
        try {
            ProductTypeEnum productTypeEnum = ProductTypeEnum.fromValue(enumValue);
        } catch(IllegalArgumentException exception) {
            assertEquals(IllegalArgumentException.class, exception.getClass());
            assertEquals("Unexpected value '" + enumValue + "'", exception.getMessage());
        }
    }
}
