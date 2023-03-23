package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddressTest {

    private String fullName;
    private String nameRow2;
    private String address;
    private String addressRow2;
    private String cap;
    private String city;
    private String city2;
    private String pr;
    private String country;
    private String flowType;
    private String productType;
    private boolean fromNationalRegistry = false;

    @BeforeEach
    void setUp(){
        this.initialize();
    }
    @Test
    void setGetTest() {
        Address adrs = initAddress();
        Assertions.assertNotNull(adrs);
        Assertions.assertFalse(adrs.isFromNationalRegistry());

        adrs = new Address();
        Assertions.assertNotNull(adrs);
        Assertions.assertNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setAddress(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setFullName(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setNameRow2(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setAddressRow2(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setCap(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setCity(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setCity2(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setPr(null);
        Assertions.assertNotNull(adrs.convertToHash());

        adrs = initAddress();
        Assertions.assertNotNull(adrs);
        adrs.setCountry(null);
        Assertions.assertNotNull(adrs.convertToHash());
    }

    private Address initAddress() {
        return new Address(fullName, nameRow2, address, addressRow2, cap, city, city2, pr, country, flowType, productType, fromNationalRegistry);
    }

    private void initialize() {
        fullName = "Ettore Fieramosca";
        nameRow2 = "nameRow2";
        address = "Via della fiera";
        addressRow2 = "Via della mosca";
        cap = "00100";
        city = "Roma";
        city2 = "Milano";
        pr = "pr";
        country = "Italy";
        flowType = Const.PREPARE;
        productType = "AAR";
        fromNationalRegistry = false;
    }
}
