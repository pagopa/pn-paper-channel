package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;


class PnRangeTest {
    private BigDecimal cost;
    private Integer minWeight;
    private Integer maxWeight;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void setGetTest() {
        // ARRANGE & ACT
        PnRange pnRange = initRange();

        //ASSERT
        Assertions.assertNotNull(pnRange);
        Assertions.assertEquals(cost, pnRange.getCost());
        Assertions.assertEquals(minWeight, pnRange.getMinWeight());
        Assertions.assertEquals(maxWeight, pnRange.getMaxWeight());


        //ARRANGE
        BigDecimal cost = BigDecimal.valueOf(2.75);
        Integer minWeight = 2;
        Integer maxWeight = 4;

        //ACT
        pnRange.setCost(cost);
        pnRange.setMinWeight(minWeight);
        pnRange.setMaxWeight(maxWeight);

        //ASSERT
        Assertions.assertEquals(cost, pnRange.getCost());
        Assertions.assertEquals(minWeight, pnRange.getMinWeight());
        Assertions.assertEquals(maxWeight, pnRange.getMaxWeight());
    }

    @Test
    void toStringTest() {
        // ARRANGE
        PnRange pnRange = initRange();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnRange.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("cost=");
        stringBuilder.append(cost);
        stringBuilder.append(", ");
        stringBuilder.append("minWeight=");
        stringBuilder.append(minWeight);
        stringBuilder.append(", ");
        stringBuilder.append("maxWeight=");
        stringBuilder.append(maxWeight);
        stringBuilder.append(")");

        //ACT
        String toTest = stringBuilder.toString();

        //ASSERT
        Assertions.assertEquals(toTest, pnRange.toString());
    }

    private PnRange initRange() {
        PnRange pnRange = new PnRange();
        pnRange.setCost(cost);
        pnRange.setMinWeight(minWeight);
        pnRange.setMaxWeight(maxWeight);
        return pnRange;
    }

    private void initialize() {
        cost = BigDecimal.valueOf(1.35);
        minWeight = 5;
        maxWeight = 8;
    }
}