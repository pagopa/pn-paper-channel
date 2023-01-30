package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ExcelModelMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<ExcelModelMapper> constructor = ExcelModelMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void excelModelMapperTest () {
        DeliveriesData response= ExcelModelMapper.fromDeliveriesDrivers(getDrivers("12345"),getCosts());
        Assertions.assertNotNull(response);
    }
    @Test
    void excelModelMapperEmptyTest () {
        DeliveriesData response= ExcelModelMapper.fromDeliveriesDrivers(getDrivers(""),getCosts());
        Assertions.assertNotNull(response);
    }
    public List<PnPaperCost> getCosts(){
        List<PnPaperCost> paperCostList= new ArrayList<>();
        PnPaperCost cost = new PnPaperCost();
        cost.setIdDeliveryDriver("12345");
        cost.setUuid("12345");
        cost.setProductType("AR");
        cost.setCap("00061");
        cost.setZone("roma");
        cost.setTenderCode("GARA-2022");
        cost.setPagePrice(0.5F);
        cost.setBasePrice(0.1F);
        paperCostList.add(cost);
        return paperCostList;
    }
    public List<PnPaperDeliveryDriver> getDrivers(String uniqueCode){
        List<PnPaperDeliveryDriver> drivers = new ArrayList<>();
        PnPaperDeliveryDriver pnPaperDeliveryDriver = new PnPaperDeliveryDriver();
        pnPaperDeliveryDriver.setFiscalCode("FRDYVB568501A");
        pnPaperDeliveryDriver.setUniqueCode(uniqueCode);
        pnPaperDeliveryDriver.setTenderCode("GARA-2022");
        pnPaperDeliveryDriver.setDenomination("denomination");
        pnPaperDeliveryDriver.setTaxId("12345");
        pnPaperDeliveryDriver.setPhoneNumber("3397755223");
        pnPaperDeliveryDriver.setFsu(true);
        pnPaperDeliveryDriver.setBusinessName("develop");
        pnPaperDeliveryDriver.setRegisteredOffice("roma");
        pnPaperDeliveryDriver.setPec("mario@pec.it");
        pnPaperDeliveryDriver.setAuthor("author");
        pnPaperDeliveryDriver.setStartDate(new Date().toInstant());
        drivers.add(pnPaperDeliveryDriver);
        return drivers;
    }
}
