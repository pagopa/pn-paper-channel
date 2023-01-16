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

@Slf4j
class AddressDAOTest extends BaseTest {

    @Autowired
    private AddressDAO addressDAO;


    @Test
    void testAddress(){
        PnAddress address = new PnAddress();
        address.setAddress("Pippo");
        address.setCap("Ciccio");
        address.setRequestId("123456566wff");
        addressDAO.create(address).block();
        PnAddress pnAddressFromDb = addressDAO.findByRequestId(address.getRequestId()).block();
        Assertions.assertNotNull(pnAddressFromDb);
    }

    @Test
    void testExistAddress(){
        PnAddress address = new PnAddress();
        address.setAddress("Pippo");
        address.setCap("Ciccio");
        address.setRequestId("123456566wffgggg");
        addressDAO.create(address).block();

        PnAddress address2 = new PnAddress();
        address2.setAddress("Pippo");
        address2.setCap("Ciccio");
        address2.setRequestId("123456566wffgggg");
        StepVerifier.create(addressDAO.create(address2)).expectError(PnHttpResponseException.class).verify();
    }
}
