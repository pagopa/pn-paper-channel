package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryDriverDAOTestIT extends BaseTest {
    @Autowired
    private DeliveryDriverDAO deliveryDriverDAO;
    private final PnDeliveryDriver entity = new PnDeliveryDriver();
    private final PnDeliveryDriver updateEntity = new PnDeliveryDriver();

    @BeforeEach
    public void setUp(){
        initialize();
    }

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

    @Test
    void getDeliveryDriverTest(){
        String tenderCode = entity.getTenderCode();
        String taxId = entity.getTaxId();
        PnDeliveryDriver pnDeliveryDriver = deliveryDriverDAO.getDeliveryDriver(tenderCode, taxId).block();
        assertNotNull(pnDeliveryDriver);
    }

    @Test
    void createOrUpdateTest(){
        PnDeliveryDriver deliveryDriver = this.deliveryDriverDAO.createOrUpdate(updateEntity).block();
        assertNotNull(deliveryDriver);
    }

    @Test
    void deleteDeliveryDriver(){
        String tenderCode = entity.getTenderCode();
        String taxId = entity.getTaxId();
        PnDeliveryDriver pnDeliveryDriver = deliveryDriverDAO.deleteDeliveryDriver(tenderCode, taxId).block();
        assertNotNull(pnDeliveryDriver);
    }

    private void initialize(){

        entity.setTenderCode("GARA-2023");
        entity.setTaxId("0123456789");
        entity.setStartDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        entity.setFsu(true);
        entity.setAuthor(Const.PN_PAPER_CHANNEL);
        entity.setDenomination("UPS");
        entity.setBusinessName("gara-2023");
        this.deliveryDriverDAO.createOrUpdate(entity).block();

        updateEntity.setTenderCode("GARA-2023");
        updateEntity.setTaxId("0123456789");
        updateEntity.setStartDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        updateEntity.setFsu(true);
        updateEntity.setAuthor(Const.PN_PAPER_CHANNEL);
        updateEntity.setDenomination("UPS");
        updateEntity.setBusinessName("gara-2023");
    }
}
