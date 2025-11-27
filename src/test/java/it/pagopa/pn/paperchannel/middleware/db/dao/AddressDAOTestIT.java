package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AddressDAOTestIT extends BaseTest {

    @Autowired
    private AddressDAO addressDAO;

    private final PnAddress address = new PnAddress();
    private final PnAddress address1 = new PnAddress();
    private final PnAddress address2 = new PnAddress();

    @BeforeEach
    void setUp(){
        initialize();
    }

    @Test
    void testAddress(){
        PnAddress address = new PnAddress();
        address.setAddress("Via Cristoforo Colombo");
        address.setCap("21047");
        address.setRequestId("abc-234-1df");
        address.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
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
        address.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        addressDAO.create(address).block();

        PnAddress address2 = new PnAddress();
        address2.setAddress("San Cristoforo");
        address2.setCap("21004");
        address2.setRequestId(address.getRequestId());
        address2.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        address2 = addressDAO.create(address2).block();

        PnAddress pnAddressFromDb = addressDAO.findByRequestId(address.getRequestId()).block();
        assertNotNull(address2);
        assertNotNull(pnAddressFromDb);
    }

    @Test
    void findByRequestIdTest(){
        PnAddress pnAddress = this.addressDAO.findByRequestId(address.getRequestId()).block();
        assertNotNull(pnAddress);
        assertEquals(pnAddress.getAddress(), address.getAddress());
        assertEquals(pnAddress.getCap(), address.getCap());
        assertEquals(pnAddress.getRequestId(), address.getRequestId());
        assertEquals(pnAddress.getTypology(), address.getTypology());
    }

    @Test
    void getPnAddressTest(){
        PnAddress pnAddress = this.addressDAO.getPnAddress(address.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, true).block();
        assertNotNull(pnAddress);
        assertEquals(pnAddress.getAddress(), address.getAddress());
        assertEquals(pnAddress.getCap(), address.getCap());
        assertEquals(pnAddress.getRequestId(), address.getRequestId());
        assertEquals(pnAddress.getTypology(), address.getTypology());
    }

    @Test
    void findAllByRequestIdTest(){
        List<PnAddress> addressList = this.addressDAO.findAllByRequestId(address1.getRequestId()).block();
        assertNotNull(addressList);
        assertEquals(2, addressList.size());

    }

    private void initialize(){

        address.setAddress("Via Aldo Moro");
        address.setCap("21004");
        address.setRequestId("LOP-DF3-412");
        address.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        this.addressDAO.create(address).block();

        address1.setAddress("San Cristoforo");
        address1.setCap("21023");
        address1.setRequestId("PLG-DR5-455");
        address1.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        this.addressDAO.create(address1).block();

        address2.setAddress("Via dalle Palle");
        address2.setCap("21023");
        address2.setRequestId("PLG-DR5-455");
        address2.setTypology(AddressTypeEnum.AR_ADDRESS.name());
        this.addressDAO.create(address2).block();


    }

}
