package it.pagopa.pn.paperchannel.model;

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
}
