package it.pagopa.pn.paperchannel.utils.costutils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneProductType {
    private String zone;
    private String productType;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZoneProductType that = (ZoneProductType) o;
        return zone.equals(that.zone) && productType.equals(that.productType) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(zone.concat(productType));
    }
}
