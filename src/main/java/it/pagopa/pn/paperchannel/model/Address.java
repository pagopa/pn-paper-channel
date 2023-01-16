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
        StringBuilder builder = new StringBuilder();
        builder.append(Utility.convertToHash(this.address));
        builder.append(Utility.convertToHash(this.fullName));
        builder.append(Utility.convertToHash(this.nameRow2));
        builder.append(Utility.convertToHash(this.addressRow2));
        builder.append(Utility.convertToHash(this.cap));
        builder.append(Utility.convertToHash(this.city));
        builder.append(Utility.convertToHash(this.city2));
        builder.append(Utility.convertToHash(this.pr));
        builder.append(Utility.convertToHash(this.country));
        return builder.toString();
    }
}
