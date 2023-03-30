package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class PnCapTest {
    public String author;
    public String cap;
    public String city;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnCap pnCap = initCap();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnCap.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("author=");
        stringBuilder.append(author);
        stringBuilder.append(", ");
        stringBuilder.append("cap=");
        stringBuilder.append(cap);
        stringBuilder.append(", ");
        stringBuilder.append("city=");
        stringBuilder.append(city);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnCap.toString());
    }

    private PnCap initCap() {
        PnCap pnCap = new PnCap();
        pnCap.setAuthor(author);
        pnCap.setCap(cap);
        pnCap.setCity(city);
        return pnCap;
    }

    private void initialize() {
        author = Const.PN_PAPER_CHANNEL;
        cap = "00100";
        city = "Roma";
    }
}
