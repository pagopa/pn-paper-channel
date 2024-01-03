package it.pagopa.pn.paperchannel.utils.costutils;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostRangesTest {

    @ParameterizedTest
    @CsvSource({
            "18, 1",
            "30, 2",
            "80, 3",
            "250, 4",
            "330, 5",
            "900, 6",
            "1900, 7",
    })
    void getBasePriceForWeightWithFirstRangeTest(int weight, int priceForWeight) {
        CostDTO costDTO = buildCostDTO();
        BigDecimal result = CostRanges.getBasePriceForWeight(costDTO, weight);

        assertThat(result).isEqualTo(BigDecimal.valueOf(priceForWeight));
    }

    @Test
    void getBasePriceForWeightWithOutOfRangeTest() {
        int weight = 2001;
        CostDTO costDTO = buildCostDTO();
        assertThatThrownBy(() -> CostRanges.getBasePriceForWeight(costDTO, weight))
                .isInstanceOf(PnGenericException.class);
    }

    private CostDTO buildCostDTO() {
        return new CostDTO()
                .price(BigDecimal.valueOf(1))
                .price50(BigDecimal.valueOf(2))
                .price100(BigDecimal.valueOf(3))
                .price250(BigDecimal.valueOf(4))
                .price350(BigDecimal.valueOf(5))
                .price1000(BigDecimal.valueOf(6))
                .price2000(BigDecimal.valueOf(7));

    }
}
