package it.pagopa.pn.paperchannel.utils.costutils;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CapProductType {
    private String cap;
    private String productType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CapProductType that = (CapProductType) o;
        return cap.equals(that.cap) && productType.equals(that.productType) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cap.concat(productType));
    }
}
