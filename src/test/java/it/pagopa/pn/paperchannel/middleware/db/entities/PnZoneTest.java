package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PnZoneTest {
    public String countryIt;
    public String countryEn;
    public String zone;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnZone pnZone = initZone();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnZone.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("countryIt=");
        stringBuilder.append(countryIt);
        stringBuilder.append(", ");
        stringBuilder.append("countryEn=");
        stringBuilder.append(countryEn);
        stringBuilder.append(", ");
        stringBuilder.append("zone=");
        stringBuilder.append(zone);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnZone.toString());
    }

    private PnZone initZone() {
        PnZone pnZone = new PnZone();
        pnZone.setCountryIt(countryIt);
        pnZone.setCountryEn(countryEn);
        pnZone.setZone(zone);
        return pnZone;
    }

    private void initialize() {
        countryIt = "Italy";
        countryEn = "Germany";
        zone = "ZONE_1";
    }
}
