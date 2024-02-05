package it.pagopa.pn.paperchannel.utils.costutils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CostUtilsTest {

    @ParameterizedTest
    @CsvSource(value = {
            "22, 436, 532", // Round Up Test
            "22, 397, 484", // Round Down Test
            "22, 1000, 1220", // 22% VAT
            "10, 1500, 1650", // 10% VAT
            "0, 1500, 1500", // Test with 0% VAT
            "NULL, 1500, 1500" // Test with null VAT
    }, nullValues = {"NULL"})
    void getCostWithVatTest(Integer vat, Integer cost, Integer expectedCostWithVat) {

        Integer costWithVat = CostUtils.getCostWithVat(vat, cost);

        Assertions.assertNotNull(costWithVat);
        Assertions.assertEquals(expectedCostWithVat, costWithVat);
    }
}
