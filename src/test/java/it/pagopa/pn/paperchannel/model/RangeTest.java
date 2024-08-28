package it.pagopa.pn.paperchannel.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;


class RangeTest {
    private BigDecimal cost;
    private Integer minWeight;
    private Integer maxWeight;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void setGetTest() {
        //ARRANGE
        Range range = initRange();
        Assertions.assertNotNull(range);
        Assertions.assertEquals(cost, range.getCost());
        Assertions.assertEquals(minWeight, range.getMinWeight());
        Assertions.assertEquals(maxWeight, range.getMaxWeight());

        BigDecimal cost = BigDecimal.valueOf(2.75);
        Integer minWeight = 2;
        Integer maxWeight = 4;

        //ACT
        range.setCost(cost);
        range.setMinWeight(minWeight);
        range.setMaxWeight(maxWeight);

        //ASSERT
        Assertions.assertEquals(cost, range.getCost());
        Assertions.assertEquals(minWeight, range.getMinWeight());
        Assertions.assertEquals(maxWeight, range.getMaxWeight());

    }

    private Range initRange() {
        return new Range(cost, minWeight, maxWeight);
    }

    private void initialize() {
        cost = BigDecimal.valueOf(1.35);
        minWeight = 5;
        maxWeight = 8;
    }
}
