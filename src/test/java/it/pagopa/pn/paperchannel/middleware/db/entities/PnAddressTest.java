package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PnAddressTest {
    private String requestId;
    private String fullName;
    private String nameRow2;
    private String address;
    private String addressRow2;
    private String cap;
    private String city;
    private String city2;
    private String pr;
    private String country;
    private Long ttl;
    private String typology;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    private PnAddress initAddress() {
        PnAddress pnAddress = new PnAddress();
        pnAddress.setRequestId(requestId);
        pnAddress.setFullName(fullName);
        pnAddress.setNameRow2(nameRow2);
        pnAddress.setAddress(address);
        pnAddress.setAddressRow2(addressRow2);
        pnAddress.setCap(cap);
        pnAddress.setCity(city);
        pnAddress.setCity2(city2);
        pnAddress.setPr(pr);
        pnAddress.setCountry(country);
        pnAddress.setTtl(ttl);
        pnAddress.setTypology(typology);

        return pnAddress;
    }

    @Test
    void toStringTest() {
        PnAddress pnAddress = initAddress();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnAddress.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("requestId=");
        stringBuilder.append(requestId);
        stringBuilder.append(", ");
        stringBuilder.append("fullName=");
        stringBuilder.append(fullName);
        stringBuilder.append(", ");
        stringBuilder.append("nameRow2=");
        stringBuilder.append(nameRow2);
        stringBuilder.append(", ");
        stringBuilder.append("address=");
        stringBuilder.append(address);
        stringBuilder.append(", ");
        stringBuilder.append("addressRow2=");
        stringBuilder.append(addressRow2);
        stringBuilder.append(", ");
        stringBuilder.append("cap=");
        stringBuilder.append(cap);
        stringBuilder.append(", ");
        stringBuilder.append("city=");
        stringBuilder.append(city);
        stringBuilder.append(", ");
        stringBuilder.append("city2=");
        stringBuilder.append(city2);
        stringBuilder.append(", ");
        stringBuilder.append("pr=");
        stringBuilder.append(pr);
        stringBuilder.append(", ");
        stringBuilder.append("country=");
        stringBuilder.append(country);
        stringBuilder.append(", ");
        stringBuilder.append("ttl=");
        stringBuilder.append(ttl);
        stringBuilder.append(", ");
        stringBuilder.append("typology=");
        stringBuilder.append(typology);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnAddress.toString());
    }

    private void initialize() {
        requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        fullName = "Ettore Fieramosca";
        nameRow2 = "nameRow2";
        address = "Via della fiera";
        addressRow2 = "Via della mosca";
        cap = "00100";
        city = "Roma";
        city2 = "Milano";
        pr = "pr";
        country = "Italy";
        ttl = 10L;
        typology = "typology";
    }
}
