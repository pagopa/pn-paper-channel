package it.pagopa.pn.paperchannel.utils.costutils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.swing.text.StyledEditorKit;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneProductType {
    private String zone;
    private String productType;
    private boolean fsu;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZoneProductType that = (ZoneProductType) o;
        return zone.equals(that.zone) && productType.equals(that.productType) && Boolean.compare(fsu, that.fsu) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(zone.concat(productType));
    }
}
