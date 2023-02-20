package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DeliveryDriverMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<DeliveryDriverMapper> constructor = DeliveryDriverMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void deliveryDriverMapperTest() {
        DeliveryDriverDTO response= DeliveryDriverMapper.deliveryDriverToDto(getPnPaperDeliveryDriver() );
        Assertions.assertNotNull(response);
    }

    @Test
    void deliveryDriverToPageableResponseTest() {
        Pageable pageable =Mockito.mock(Pageable.class, Mockito.CALLS_REAL_METHODS);
        List<PnDeliveryDriver> list= new ArrayList<>();
        PageableDeliveryDriverResponseDto response= DeliveryDriverMapper.toPageableResponse(DeliveryDriverMapper.toPagination(pageable, list));
        Assertions.assertNotNull(response);
    }

    @Test
    void deliveryDriverToPaginationTest() {
        Pageable pageable =Mockito.mock(Pageable.class, Mockito.CALLS_REAL_METHODS);
        List<PnDeliveryDriver> list= new ArrayList<>();
        PageModel<PnDeliveryDriver> response= DeliveryDriverMapper.toPagination(pageable, list);

        Assertions.assertNotNull(response);
    }




    @Test
    void toEntityFromExcelTest(){
        DeliveryDriverMapper.toEntityFromExcel(getDeliveries(), "ABC");
    }

    private DeliveriesData getDeliveries() {
        DeliveriesData deliveriesData = new DeliveriesData();
        DeliveryAndCost d1 = new DeliveryAndCost();
        d1.setUniqueCode("AAAAA");
        d1.setTaxId("ID-1");
        d1.setProductType("AR");
        d1.setCaps(List.of("81029, 23945"));
        d1.setFsu(false);
        DeliveryAndCost d2 = new DeliveryAndCost();
        d2.setUniqueCode("AA3AA");
        d2.setTaxId("ID-3");
        d2.setProductType("AR");
        d2.setCaps(List.of("81029, 43945"));
        d2.setFsu(false);
        DeliveryAndCost d3 = new DeliveryAndCost();
        d3.setUniqueCode("AAAAA");
        d3.setTaxId("ID-1");
        d3.setProductType("890");
        d3.setZone("ZONE_1");
        d3.setFsu(false);
        DeliveryAndCost d4 = new DeliveryAndCost();
        d4.setUniqueCode("AAAAA");
        d4.setTaxId("ID-1");
        d4.setProductType("SEMPLICE");
        d4.setCaps(List.of("81029, 23945"));
        d4.setFsu(false);
        DeliveryAndCost d5 = new DeliveryAndCost();
        d5.setUniqueCode("AAAAA");
        d5.setTaxId("ID-1");
        d5.setProductType("890");
        d5.setZone("ZONE_1");
        d5.setFsu(false);
        deliveriesData.setDeliveriesAndCosts(List.of(d1, d2,d3,d4,d5));
        return deliveriesData;
    }

    private PnDeliveryDriver getPnPaperDeliveryDriver() {
        PnDeliveryDriver pnDeliveryDriver = new PnDeliveryDriver();
        pnDeliveryDriver.setFiscalCode("FRDYVB568501A");
        pnDeliveryDriver.setUniqueCode("123456");
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
        return pnDeliveryDriver;
    }
}
