package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.paperchannel.LocalStackTestConfig;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class AddressDAOTest extends BaseTest {

    @Autowired
    private AddressDAO addressDAO;


    @Test
    void testAddress(){
        PnAddress address = new PnAddress();
        address.setAddress("Via Cristoforo Colombo");
        address.setCap("21047");
        address.setRequestId("abc-234-1df");
        addressDAO.create(address).block();
        PnAddress pnAddressFromDb = addressDAO.findByRequestId(address.getRequestId()).block();
        assertNotNull(pnAddressFromDb);
        assertEquals(address.getAddress(), pnAddressFromDb.getAddress());
        assertEquals(address.getCap(), pnAddressFromDb.getCap());
    }

    @Test
    void addressOverwrittenTest(){
        PnAddress address = new PnAddress();
        address.setAddress("Via Aldo Moro");
        address.setCap("21004");
        address.setRequestId("LOP-DF3-412");
        addressDAO.create(address).block();

        PnAddress address2 = new PnAddress();
        address2.setAddress("San Cristoforo");
        address2.setCap("21004");
        address2.setRequestId(address.getRequestId());
        address2 = addressDAO.create(address2).block();

        PnAddress pnAddressFromDb = addressDAO.findByRequestId(address.getRequestId()).block();
        assertNotNull(address2);
        assertNotNull(pnAddressFromDb);
    }
}
