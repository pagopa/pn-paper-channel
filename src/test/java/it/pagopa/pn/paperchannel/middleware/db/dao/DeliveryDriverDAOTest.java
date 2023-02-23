package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DeliveryDriverDAOTest extends BaseTest {
    @Autowired
    private DeliveryDriverDAO deliveryDriverDAO;


    @Test
    void getDeliveryDriverFromTenderSuccess() {
        String tenderCode = "GARA-2022";
        Boolean onlyFSU = false;

        Flux<PnDeliveryDriver> fluxDeliveryDriver = deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode, onlyFSU);
        String fTC1 = tenderCode;
        fluxDeliveryDriver.collectList()
                .flatMap(driverList -> {
                    PnDeliveryDriver pnDeliveryDriver = (PnDeliveryDriver) driverList.stream().filter(driver -> driver.getUniqueCode().equals("LOP3222"));
                    assertEquals(fTC1, pnDeliveryDriver.getTenderCode());
                    assertEquals("BRT", pnDeliveryDriver.getDenomination());
                    assertEquals(false, pnDeliveryDriver.getFsu());
                    assertEquals("21432432342", pnDeliveryDriver.getTaxId());

                    pnDeliveryDriver = (PnDeliveryDriver) driverList.stream().filter(driver -> driver.getUniqueCode().equals("CXJ664"));
                    assertEquals(fTC1, pnDeliveryDriver.getTenderCode());
                    assertEquals("NEXIVE", pnDeliveryDriver.getDenomination());
                    assertEquals(false, pnDeliveryDriver.getFsu());
                    assertEquals("12312434324", pnDeliveryDriver.getTaxId());

                    pnDeliveryDriver = (PnDeliveryDriver) driverList.stream().filter(driver -> driver.getUniqueCode().equals("CXJ564"));
                    assertEquals(fTC1, pnDeliveryDriver.getTenderCode());
                    assertEquals("GLS", pnDeliveryDriver.getDenomination());
                    assertEquals(false, pnDeliveryDriver.getFsu());
                    assertEquals("12349574832", pnDeliveryDriver.getTaxId());

                    return Mono.empty();
                });

        tenderCode = "GARA-2023";
        onlyFSU = true;
        fluxDeliveryDriver = deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode, onlyFSU);
        String fTC2 = tenderCode;
        fluxDeliveryDriver.collectList()
                .flatMap(driverList -> {
                    PnDeliveryDriver pnDeliveryDriver = (PnDeliveryDriver) driverList.stream().filter(driver -> driver.getUniqueCode().equals("KAS1901"));
                    assertEquals(fTC2, pnDeliveryDriver.getTenderCode());
                    assertEquals("UPS", pnDeliveryDriver.getDenomination());
                    assertEquals(true, pnDeliveryDriver.getFsu());
                    assertEquals("0123456789", pnDeliveryDriver.getTaxId());

                    return Mono.empty();
                });

        tenderCode = "GARA-2023";
        onlyFSU = null;
        fluxDeliveryDriver = deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode, onlyFSU);
        String fTC3 = tenderCode;
        fluxDeliveryDriver.collectList()
                .flatMap(driverList -> {
                    PnDeliveryDriver pnDeliveryDriver = (PnDeliveryDriver) driverList.stream().filter(driver -> driver.getUniqueCode().equals("KAS1901"));
                    assertEquals(fTC3, pnDeliveryDriver.getTenderCode());
                    assertEquals("UPS", pnDeliveryDriver.getDenomination());
                    assertEquals("0123456789", pnDeliveryDriver.getTaxId());

                    return Mono.empty();
                });
    }

    @Test
    void getDeliveryDriverFSUSuccess() {
        String tenderCode = "GARA-2023";
        PnDeliveryDriver pnDeliveryDriver = deliveryDriverDAO.getDeliveryDriverFSU(tenderCode).block();
        assertEquals(tenderCode, pnDeliveryDriver.getTenderCode());
        assertEquals("UPS", pnDeliveryDriver.getDenomination());
        assertEquals(true, pnDeliveryDriver.getFsu());
        assertEquals("0123456789", pnDeliveryDriver.getTaxId());

        tenderCode = "GARA-2000";
        pnDeliveryDriver = deliveryDriverDAO.getDeliveryDriverFSU(tenderCode).block();
        assertNull(pnDeliveryDriver);

    }
}
