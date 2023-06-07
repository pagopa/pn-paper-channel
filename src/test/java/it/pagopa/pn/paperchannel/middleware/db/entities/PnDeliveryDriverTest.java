package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;

class PnDeliveryDriverTest {
    private PnDeliveryDriver pnDeliveryDriver;
    private PnDeliveryDriver toPnDeliveryDriver;
    private String author;
    private boolean fsu;
    private String taxId;
    private String tenderCode;
    private Instant startDate;
    private String denomination;
    private String registeredOffice;
    private String pec;
    private String uniqueCode;
    private String businessName;
    private String phoneNumber;
    private String fiscalCode;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void isEqualsTest() {
        pnDeliveryDriver = initDeliveryDriver();
        boolean isEquals = pnDeliveryDriver.equals(toPnDeliveryDriver);
        Assertions.assertFalse(isEquals);

        PnDeliveryDriver fakePnCost = new PnDeliveryDriver();
        isEquals = pnDeliveryDriver.equals(fakePnCost);
        Assertions.assertFalse(isEquals);

        isEquals = pnDeliveryDriver.equals(pnDeliveryDriver);
        Assertions.assertTrue(isEquals);

        PnCost fakePnDeliveryDriver = new PnCost();
        isEquals = pnDeliveryDriver.equals(fakePnDeliveryDriver);
        Assertions.assertFalse(isEquals);

        toPnDeliveryDriver = initDeliveryDriver();
        toPnDeliveryDriver.setFsu(false);
        isEquals = pnDeliveryDriver.equals(toPnDeliveryDriver);
        Assertions.assertTrue(isEquals);
    }

    @Test
    void toStringTest() {
        pnDeliveryDriver = initDeliveryDriver();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnDeliveryDriver.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("uniqueCode=");
        stringBuilder.append(uniqueCode);
        stringBuilder.append(", ");
        stringBuilder.append("tenderCode=");
        stringBuilder.append(tenderCode);
        stringBuilder.append(", ");
        stringBuilder.append("denomination=");
        stringBuilder.append(denomination);
        stringBuilder.append(", ");
        stringBuilder.append("fsu=");
        stringBuilder.append(fsu);
        stringBuilder.append(", ");
        stringBuilder.append("author=");
        stringBuilder.append(author);
        stringBuilder.append(", ");
        stringBuilder.append("startDate=");
        stringBuilder.append(startDate);
        stringBuilder.append(", ");
        stringBuilder.append("businessName=");
        stringBuilder.append(businessName);
        stringBuilder.append(", ");
        stringBuilder.append("registeredOffice=");
        stringBuilder.append(registeredOffice);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnDeliveryDriver.toString());
    }
    private PnDeliveryDriver initDeliveryDriver() {
        PnDeliveryDriver pnDeliveryDriver = new PnDeliveryDriver();
        pnDeliveryDriver.setFsu(fsu);
        pnDeliveryDriver.setTaxId(taxId);
        pnDeliveryDriver.setTenderCode(tenderCode);
        pnDeliveryDriver.setStartDate(startDate);
        pnDeliveryDriver.setDenomination(denomination);
        pnDeliveryDriver.setRegisteredOffice(registeredOffice);
        pnDeliveryDriver.setPec(pec);
        pnDeliveryDriver.setAuthor(author);
        pnDeliveryDriver.setUniqueCode(uniqueCode);
        pnDeliveryDriver.setBusinessName(businessName);
        pnDeliveryDriver.setPhoneNumber(phoneNumber);
        pnDeliveryDriver.setFiscalCode(fiscalCode);
        return pnDeliveryDriver;
    }

    private void initialize() {
        author = "THE BOSS";
        fsu = true;
        taxId = "5432106789";
        tenderCode = "XSCDVFBGNHMJ";
        startDate = Instant.now();
        denomination = "BUSINESS S.R.L.";
        registeredOffice = "REGISTERED OFFICE";
        pec = "business@pec.it";
        uniqueCode = "VFBGHYJUKILO";
        businessName = "BUSINESS";
        phoneNumber = "06987654321";
        fiscalCode = "ABCDEF98G76H543I";
    }
}
