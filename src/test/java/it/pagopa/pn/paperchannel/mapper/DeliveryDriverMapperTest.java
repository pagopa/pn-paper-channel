package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

class DeliveryDriverMapperTest {

    @Test
    void deliveryDriverMapperTest() {
        DeliveryDriverDto response= DeliveryDriverMapper.deliveryDriverToDto(getPnPaperDeliveryDriver() );
        Assertions.assertNotNull(response);
    }
    /*@Test
    void deliveryDriverToPageableResponseTest() {
        PageableDeliveryDriverResponseDto response= DeliveryDriverMapper.toPageableResponse();
        Assertions.assertNotNull(response);
    }
    @Test
    void deliveryDriverToPaginationTest() {
        PageModel<PnPaperDeliveryDriver> response= DeliveryDriverMapper.toPagination();
        Assertions.assertNotNull(response);
    }*/

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
