package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelAddress;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.time.Instant;

import static it.pagopa.pn.paperchannel.utils.AddressTypeEnum.RECEIVER_ADDRESS;
import static it.pagopa.pn.paperchannel.utils.AddressTypeEnum.SENDER_ADDRES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PnPaperChannelAddressDAOTestIT extends BaseTest {

    @Autowired
    private PnPaperChannelAddressDAO paperChannelAddressDAO;

    @Test
    void testCreatePaperChannelAddress() {
        PnPaperChannelAddress pnPaperChannelAddress = createPaperChannelAddress("TEST_REQUEST_ID_CREATE", RECEIVER_ADDRESS);

        StepVerifier.create(paperChannelAddressDAO.create(pnPaperChannelAddress))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(pnPaperChannelAddress.getRequestId(), result.getRequestId());
                    assertEquals(pnPaperChannelAddress.getAddressId(), result.getAddressId());
                })
                .verifyComplete();
    }

    @Test
    void testCreateAndGetPaperAddress() {
        PnPaperChannelAddress pnPaperChannelAddress = createPaperChannelAddress("TEST_REQUEST_ID", RECEIVER_ADDRESS);
        paperChannelAddressDAO.create(pnPaperChannelAddress).block();

        StepVerifier.create(paperChannelAddressDAO.findByRequestId(
                        pnPaperChannelAddress.getRequestId(), RECEIVER_ADDRESS))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(pnPaperChannelAddress.getCap(), result.getCap());
                    assertEquals(pnPaperChannelAddress.getCity(), result.getCity());
                })
                .verifyComplete();
    }

    @Test
    void testGetPaperAddressesByRequestId() {
        PnPaperChannelAddress pnPaperChannelAddress = createPaperChannelAddress("TEST_REQUEST_ID1", RECEIVER_ADDRESS);
        PnPaperChannelAddress pnPaperChannelAddress2 = createPaperChannelAddress("TEST_REQUEST_ID1", SENDER_ADDRES);
        paperChannelAddressDAO.create(pnPaperChannelAddress).block();
        paperChannelAddressDAO.create(pnPaperChannelAddress2).block();

        StepVerifier.create(paperChannelAddressDAO.findAllByRequestId(pnPaperChannelAddress.getRequestId()))
                .assertNext(addresses -> {
                    assertNotNull(addresses);
                    assertEquals(2, addresses.size());
                })
                .verifyComplete();
    }

    @Test
    void testGetPaperAddressByRequestId() {
        PnPaperChannelAddress pnPaperChannelAddress = createPaperChannelAddress("TEST_REQUEST_ID2", RECEIVER_ADDRESS);
        PnPaperChannelAddress pnPaperChannelAddress2 = createPaperChannelAddress("TEST_REQUEST_ID2", SENDER_ADDRES);
        paperChannelAddressDAO.create(pnPaperChannelAddress).block();
        paperChannelAddressDAO.create(pnPaperChannelAddress2).block();

        StepVerifier.create(paperChannelAddressDAO.findByRequestId(pnPaperChannelAddress.getRequestId()))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();
    }

    private PnPaperChannelAddress createPaperChannelAddress(String requestId, AddressTypeEnum addressTypeEnum) {
        PnPaperChannelAddress address = new PnPaperChannelAddress();
        address.setRequestId(requestId);
        address.setAddressId("ADDRESS_001");
        address.setAddressType(addressTypeEnum.name());
        address.setCap("00100");
        address.setCity("Roma");
        address.setPr("RM");
        address.setCountry("Italia");
        address.setTtl(Instant.now().plusSeconds(86400).getEpochSecond());
        return address;
    }


}
