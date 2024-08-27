package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;


class PnPaperChannelDeliveryDriverTest {
    private String deliveryDriverId;
    private String taxId;
    private String businessName;
    private String fiscalCode;
    private String pec;
    private String phoneNumber;
    private String registeredOffice;
    private Instant createdAt;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnPaperChannelDeliveryDriver channelDeliveryDriver = initPaperChannelDeliveryDriver();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(channelDeliveryDriver.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("deliveryDriverId=");
        stringBuilder.append(deliveryDriverId);
        stringBuilder.append(", ");
        stringBuilder.append("taxId=");
        stringBuilder.append(taxId);
        stringBuilder.append(", ");
        stringBuilder.append("businessName=");
        stringBuilder.append(businessName);
        stringBuilder.append(", ");
        stringBuilder.append("fiscalCode=");
        stringBuilder.append(fiscalCode);
        stringBuilder.append(", ");
        stringBuilder.append("pec=");
        stringBuilder.append(pec);
        stringBuilder.append(", ");
        stringBuilder.append("phoneNumber=");
        stringBuilder.append(phoneNumber);
        stringBuilder.append(", ");
        stringBuilder.append("registeredOffice=");
        stringBuilder.append(registeredOffice);
        stringBuilder.append(", ");
        stringBuilder.append("createdAt=");
        stringBuilder.append(createdAt);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, channelDeliveryDriver.toString());
    }

    private PnPaperChannelDeliveryDriver initPaperChannelDeliveryDriver() {
        PnPaperChannelDeliveryDriver channelDeliveryDriver = new PnPaperChannelDeliveryDriver();
        channelDeliveryDriver.setDeliveryDriverId(deliveryDriverId);
        channelDeliveryDriver.setTaxId(taxId);
        channelDeliveryDriver.setBusinessName(businessName);
        channelDeliveryDriver.setFiscalCode(fiscalCode);
        channelDeliveryDriver.setPec(pec);
        channelDeliveryDriver.setPhoneNumber(phoneNumber);
        channelDeliveryDriver.setRegisteredOffice(registeredOffice);
        channelDeliveryDriver.setCreatedAt(createdAt);
        return channelDeliveryDriver;
    }

    private void initialize() {
        deliveryDriverId = "deliveryDriverId";
        taxId = "1234567890";
        businessName = "businessName";
        fiscalCode = "VSCTRN95P76M123G";
        pec = "test@prova.it";
        phoneNumber = "06123456789";
        registeredOffice = "987612345";
        createdAt = Instant.now();
    }
}