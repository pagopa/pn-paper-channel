package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    private String fullName;

    private String nameRow2;

    private String address;

    private String addressRow2;

    private String cap;

    private String city;

    private String city2;

    private String pr;

    private String country;

    private boolean fromNationalRegistry = false;

    public String convertToHash() {
        if (
            this.address == null &&
            this.fullName == null &&
            this.nameRow2 == null &&
            this.addressRow2 == null &&
            this.cap == null &&
            this.city == null &&
            this.city2 == null &&
            this.pr == null &&
            this.country == null ) return null;

        return Utility.convertToHash(this.address) +
                Utility.convertToHash(this.fullName) +
                Utility.convertToHash(this.nameRow2) +
                Utility.convertToHash(this.addressRow2) +
                Utility.convertToHash(this.cap) +
                Utility.convertToHash(this.city) +
                Utility.convertToHash(this.city2) +
                Utility.convertToHash(this.pr) +
                Utility.convertToHash(this.country);
    }
}
