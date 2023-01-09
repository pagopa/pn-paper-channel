package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.LocalStackTestConfig;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(LocalStackTestConfig.class)
class AddressDAOTest {

    @Autowired
    private AddressDAO addressDAO;


    @Test
    void testAddress(){
        PnAddress address = new PnAddress();
        address.setAddress("Pippo");
        address.setCap("Ciccio");
        address.setRequestId("123456566wff");
        addressDAO.create(address).block();
    }


}
