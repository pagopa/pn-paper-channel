package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
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
        List<PnPaperDeliveryDriver> list= new ArrayList<>();
        PageableDeliveryDriverResponseDto response= DeliveryDriverMapper.toPageableResponse(DeliveryDriverMapper.toPagination(pageable, list));
        Assertions.assertNotNull(response);
    }

    @Test
    void deliveryDriverToPaginationTest() {
        Pageable pageable =Mockito.mock(Pageable.class, Mockito.CALLS_REAL_METHODS);
        List<PnPaperDeliveryDriver> list= new ArrayList<>();
        PageModel<PnPaperDeliveryDriver> response= DeliveryDriverMapper.toPagination(pageable, list);

        Assertions.assertNotNull(response);
    }

    public PnPaperDeliveryDriver getPnPaperDeliveryDriver() {
        PnPaperDeliveryDriver pnPaperDeliveryDriver = new PnPaperDeliveryDriver();
        pnPaperDeliveryDriver.setFiscalCode("FRDYVB568501A");
        pnPaperDeliveryDriver.setUniqueCode("123456");
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
        return pnPaperDeliveryDriver;
    }
}
