package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CostDAOTest extends BaseTest {

    @Autowired
    private CostDAO costDAO;
    private final PnCost costNational = new PnCost();
    private final PnCost costNationalFSU = new PnCost();
    private final PnCost costInternational = new PnCost();
    private final PnCost costInternationalFSU1 = new PnCost();
    private final PnCost costInternationalFSU2 = new PnCost();

    @BeforeEach
    public void setUp(){
        initialValue();
    }

    @Test
    void findAllFromTenderCodeTest(){
        List<PnCost> prices = this.costDAO.findAllFromTenderCode(costNational.getTenderCode(), null).collectList().block();
        assertNotNull(prices);
        assertEquals(5, prices.size());

        prices = this.costDAO.findAllFromTenderCode("TEST_ERROR_TENDER_CODE", null).collectList().block();
        assertNotNull(prices);
        assertEquals(0, prices.size());

    }

    @Test
    void findAllFromTenderCodeAndProductTypeTest(){
        List<PnCost> costs = this.costDAO
                .findAllFromTenderAndProductTypeAndExcludedUUID(
                        costNational.getTenderCode(),
                        costNational.getProductType(),
                        null
                ).block();
        assertNotNull(costs);
        assertEquals(5, costs.size());
        costs = this.costDAO
                .findAllFromTenderAndProductTypeAndExcludedUUID(
                        costNational.getTenderCode(),
                        costNational.getProductType(),
                        costNational.getUuid()
                ).block();
        assertNotNull(costs);
        assertEquals(4, costs.size());
    }

    @Test
    void getByCapOrZoneAndProductTypeTest(){
        PnCost cost = this.costDAO.getByCapOrZoneAndProductType(
                costInternationalFSU1.getTenderCode(), null, "ZONE_1", "AR").block();

        assertNotNull(cost);
        assertFalse(cost.getFsu());

        cost = this.costDAO.getByCapOrZoneAndProductType(
                costNationalFSU.getTenderCode(), "21047", null, "AR"
        ).block();
        assertNotNull(cost);
        assertFalse(cost.getFsu());

        cost = this.costDAO.getByCapOrZoneAndProductType(
                "TENDER-102", "21047", null, "AR"
        ).block();
        assertNotNull(cost);
        assertTrue(cost.getFsu());

        cost = this.costDAO.getByCapOrZoneAndProductType(
                "TENDER-102", "00010", null, "AR"
        ).block();
        assertNotNull(cost);
        assertTrue(cost.getFsu());

        cost = this.costDAO.getByCapOrZoneAndProductType(
                "TENDER-102", null, "ZONE_3", "SEMPLICE"
        ).block();
        assertNull(cost);
    }


    private void initialValue(){
        costNational.setDeliveryDriverCode("ABC-1");
        costNational.setUuid("ABC-UUID");
        costNational.setTenderCode("TENDER-1");
        costNational.setProductType("AR");
        costNational.setCap(List.of("21047", "21050", "81022", "13000"));
        costNational.setBasePrice(2.2F);
        costNational.setPagePrice(2.2F);
        costNational.setFsu(false);
        costDAO.createOrUpdate(costNational).block();
        log.info("COST NATIONAL CREATED");

        costNationalFSU.setDeliveryDriverCode("ABC-1");
        costNationalFSU.setUuid("ABC-UUID-FSU-DEFAULT");
        costNationalFSU.setTenderCode("TENDER-1");
        costNationalFSU.setProductType("AR");
        costNationalFSU.setCap(List.of("99999", "10902", "000212", "34523"));
        costNationalFSU.setBasePrice(2.2F);
        costNationalFSU.setPagePrice(2.2F);
        costNationalFSU.setFsu(false);
        costDAO.createOrUpdate(costNationalFSU).block();
        log.info("COST NATIONAL CREATED");

        PnCost c1 = new PnCost();
        c1.setDeliveryDriverCode("ABC-1l-123");
        c1.setUuid("ABC-UUID-342");
        c1.setTenderCode("TENDER-102");
        c1.setProductType("AR");
        c1.setCap(List.of("21047", "21050", "81022", "13000", "99999"));
        c1.setBasePrice(2.2F);
        c1.setPagePrice(2.2F);
        c1.setFsu(true);
        costDAO.createOrUpdate(c1).block();
        log.info("COST c1 CREATED");

        costInternational.setDeliveryDriverCode("ABC-INTER");
        costInternational.setUuid("ABC-UUID-INTER");
        costInternational.setTenderCode("TENDER-1");
        costInternational.setProductType("AR");
        costInternational.setZone("ZONE_1");
        costInternational.setBasePrice(2.2F);
        costInternational.setPagePrice(2.2F);
        costInternational.setFsu(false);
        costDAO.createOrUpdate(costInternational).block();
        log.info("COST INTERNATIONAL CREATED");


        costInternationalFSU1.setDeliveryDriverCode("ABC-INTER");
        costInternationalFSU1.setUuid("ABC-UUID-INTER-FUS-AR-2");
        costInternationalFSU1.setTenderCode("TENDER-1");
        costInternationalFSU1.setProductType("AR");
        costInternationalFSU1.setZone("ZONE_1");
        costInternationalFSU1.setBasePrice(2.2F);
        costInternationalFSU1.setPagePrice(2.2F);
        costInternationalFSU1.setFsu(true);
        costDAO.createOrUpdate(costInternationalFSU1).block();
        log.info("COST INTERNATIONAL FSU 1 CREATED");


        costInternationalFSU2.setDeliveryDriverCode("ABC-INTER");
        costInternationalFSU2.setUuid("ABC-UUID-INTER-FSU-ar-1");
        costInternationalFSU2.setTenderCode("TENDER-1");
        costInternationalFSU2.setProductType("AR");
        costInternationalFSU2.setZone("ZONE_2");
        costInternationalFSU2.setBasePrice(2.2F);
        costInternationalFSU2.setPagePrice(2.2F);
        costInternationalFSU2.setFsu(true);
        costDAO.createOrUpdate(costInternationalFSU2).block();
        log.info("COST INTERNATIONAL FSU 2 CREATED");

    }

}
