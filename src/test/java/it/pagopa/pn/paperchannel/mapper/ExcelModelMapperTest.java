package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
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
    @Test
    void excelModelMapperEmptyListCostTest () {
        DeliveriesData response= ExcelModelMapper.fromDeliveriesDrivers(getDrivers("uniqueCode"),new ArrayList<>());
        Assertions.assertNotNull(response);
    }

    public List<PnCost> getCosts(){
        List<PnCost> paperCostList= new ArrayList<>();
        PnCost cost = new PnCost();
        cost.setDeliveryDriverCode("12345");
        cost.setUuid("12345");
        cost.setProductType("AR");
        //cost.setCap("00061");
        cost.setZone("roma");
        cost.setTenderCode("GARA-2022");
        cost.setPagePrice(BigDecimal.valueOf(0.5F));
        cost.setBasePrice(BigDecimal.valueOf(0.1F));
        paperCostList.add(cost);
        return paperCostList;
    }
    public List<PnDeliveryDriver> getDrivers(String uniqueCode){
        List<PnDeliveryDriver> drivers = new ArrayList<>();
        PnDeliveryDriver pnDeliveryDriver = new PnDeliveryDriver();
        pnDeliveryDriver.setFiscalCode("FRDYVB568501A");
        pnDeliveryDriver.setUniqueCode(uniqueCode);
        pnDeliveryDriver.setTenderCode("GARA-2022");
        pnDeliveryDriver.setDenomination("denomination");
        pnDeliveryDriver.setTaxId("12345");
        pnDeliveryDriver.setPhoneNumber("3397755223");
        pnDeliveryDriver.setFsu(true);
        pnDeliveryDriver.setBusinessName("develop");
        pnDeliveryDriver.setRegisteredOffice("roma");
        pnDeliveryDriver.setPec("mario@pec.it");
        pnDeliveryDriver.setAuthor("author");
        pnDeliveryDriver.setStartDate(new Date().toInstant());
        drivers.add(pnDeliveryDriver);
        return drivers;
    }
}
