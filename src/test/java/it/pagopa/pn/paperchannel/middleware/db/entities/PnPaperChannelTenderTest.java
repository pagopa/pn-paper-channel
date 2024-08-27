package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;


class PnPaperChannelTenderTest {
    private String tenderId;
    private Instant activationDate;
    private String tenderName;
    private Integer vat;
    private Integer nonDeductibleVat;
    private BigDecimal pagePrice;
    private BigDecimal basePriceAR;
    private BigDecimal basePriceRS;
    private BigDecimal basePrice890;
    private BigDecimal fee;
    private Instant createdAt;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnPaperChannelTender channelTender = initPaperChannelTender();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(channelTender.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("tenderId=");
        stringBuilder.append(tenderId);
        stringBuilder.append(", ");
        stringBuilder.append("activationDate=");
        stringBuilder.append(activationDate);
        stringBuilder.append(", ");
        stringBuilder.append("tenderName=");
        stringBuilder.append(tenderName);
        stringBuilder.append(", ");
        stringBuilder.append("vat=");
        stringBuilder.append(vat);
        stringBuilder.append(", ");
        stringBuilder.append("nonDeductibleVat=");
        stringBuilder.append(nonDeductibleVat);
        stringBuilder.append(", ");
        stringBuilder.append("pagePrice=");
        stringBuilder.append(pagePrice);
        stringBuilder.append(", ");
        stringBuilder.append("basePriceAR=");
        stringBuilder.append(basePriceAR);
        stringBuilder.append(", ");
        stringBuilder.append("basePriceRS=");
        stringBuilder.append(basePriceRS);
        stringBuilder.append(", ");
        stringBuilder.append("basePrice890=");
        stringBuilder.append(basePrice890);
        stringBuilder.append(", ");
        stringBuilder.append("fee=");
        stringBuilder.append(fee);
        stringBuilder.append(", ");
        stringBuilder.append("createdAt=");
        stringBuilder.append(createdAt);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, channelTender.toString());
    }

    private PnPaperChannelTender initPaperChannelTender() {
        PnPaperChannelTender channelTender = new PnPaperChannelTender();
        channelTender.setTenderId(tenderId);
        channelTender.setActivationDate(activationDate);
        channelTender.setTenderName(tenderName);
        channelTender.setVat(vat);
        channelTender.setNonDeductibleVat(nonDeductibleVat);
        channelTender.setPagePrice(pagePrice);
        channelTender.setBasePriceAR(basePriceAR);
        channelTender.setBasePriceRS(basePriceRS);
        channelTender.setBasePrice890(basePrice890);
        channelTender.setFee(fee);
        channelTender.setCreatedAt(createdAt);
        return channelTender;
    }

    private void initialize() {
        tenderId = "tenderId";
        activationDate = Instant.now();
        tenderName = "tenderName";
        vat = 2;
        nonDeductibleVat = 3;
        pagePrice = BigDecimal.valueOf(0.5);
        basePriceAR = BigDecimal.valueOf(1.5);
        basePriceRS = BigDecimal.valueOf(2.5);
        basePrice890 = BigDecimal.valueOf(3.5);
        fee = BigDecimal.valueOf(0.2);
        createdAt = Instant.now();
    }
}