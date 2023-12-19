package it.pagopa.pn.paperchannel.utils.costutils;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class CostRanges {

    private CostRanges() {}

    private static final int FIRST_RANGE = 20;
    private static final int SECOND_RANGE = 50;
    private static final int THIRD_RANGE = 100;
    private static final int FOURTH_RANGE = 250;
    private static final int FIFTH_RANGE = 350;
    private static final int SIXTH_RANGE = 1000;
    private static final int SEVENTH_RANGE = 2000;

    public static BigDecimal getBasePriceForWeight(CostDTO costDTO, int totPagesWight) {
        TreeMap<Integer, BigDecimal> priceMap = buildPriceMap(costDTO);
        return priceMap.entrySet().stream()
                .filter(entry -> totPagesWight <= entry.getKey())
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new RuntimeException(String.format("Weight %s exceeded 2000 gr", totPagesWight)));
    }

    private static TreeMap<Integer, BigDecimal> buildPriceMap(CostDTO costDTO) {
        var map = new TreeMap<Integer, BigDecimal>();
        map.put(FIRST_RANGE, costDTO.getPrice());
        map.put(SECOND_RANGE, costDTO.getPrice50());
        map.put(THIRD_RANGE, costDTO.getPrice100());
        map.put(FOURTH_RANGE, costDTO.getPrice250());
        map.put(FIFTH_RANGE, costDTO.getPrice350());
        map.put(SIXTH_RANGE, costDTO.getPrice1000());
        map.put(SEVENTH_RANGE, costDTO.getPrice2000());

        return map;
    }

}
