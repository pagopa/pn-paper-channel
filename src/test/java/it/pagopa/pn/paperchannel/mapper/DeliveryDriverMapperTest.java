package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDto;
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
        DeliveryDriverDto response= DeliveryDriverMapper.deliveryDriverToDto(getPnPaperDeliveryDriver() );
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

    public PnDeliveryDriver getPnPaperDeliveryDriver() {
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
