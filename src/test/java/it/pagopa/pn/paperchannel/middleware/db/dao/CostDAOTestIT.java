package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CostDAOTestIT extends BaseTest {

    @Autowired
    private CostDAO costDAO;
    private final PnCost costNational = new PnCost();
    private final PnCost costNationalFSU = new PnCost();
    private final PnCost costInternationalFSU1 = new PnCost();
    private final PnCost costInternationalFSU2 = new PnCost();

    @BeforeEach
    public void setUp() throws ParseException {
        initialValue();
    }

    @Test
    void findAllFromTenderCodeTest(){
        List<PnCost> prices = this.costDAO.findAllFromTenderCode(costNational.getTenderCode(), null).collectList().block();
        assertNotNull(prices);
        assertEquals(4, prices.size());

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
                ).collectList().block();
        assertNotNull(costs);
        assertEquals(4, costs.size());
        costs = this.costDAO
                .findAllFromTenderAndProductTypeAndExcludedUUID(
                        costNational.getTenderCode(),
                        costNational.getProductType(),
                        costNational.getUuid()
                ).collectList().block();
        assertNotNull(costs);
        assertEquals(3, costs.size());
    }

    @Test
    void getByCapOrZoneAndProductTypeTest(){
        PnCost cost = this.costDAO.getByCapOrZoneAndProductType(
                costInternationalFSU1.getTenderCode(), null, "ZONE_1", "AR").block();

        assertNotNull(cost);
        assertTrue(cost.getFsu());

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


    private void initialValue() throws ParseException {
        costNational.setDeliveryDriverCode("ABC-1");
        costNational.setUuid("ABC-UUID");
        costNational.setTenderCode("TENDER-1");
        costNational.setProductType("AR");
        costNational.setCap(List.of("21047", "21050", "81022", "13000"));
        costNational.setBasePrice(BigDecimal.valueOf(2.20));
        costNational.setPagePrice(BigDecimal.valueOf(2.20));
        costNational.setFsu(false);
        costDAO.createOrUpdate(costNational).block();
        log.info("COST NATIONAL CREATED");

        costNationalFSU.setDeliveryDriverCode("ABC-1");
        costNationalFSU.setUuid("ABC-UUID-FSU-DEFAULT");
        costNationalFSU.setTenderCode("TENDER-1");
        costNationalFSU.setProductType("AR");
        costNationalFSU.setCap(List.of("99999", "10902", "000212", "34523"));
        costNationalFSU.setBasePrice(BigDecimal.valueOf(2.25));
        costNationalFSU.setPagePrice(BigDecimal.valueOf(2.25));
        costNationalFSU.setFsu(false);
        costDAO.createOrUpdate(costNationalFSU).block();
        log.info("COST NATIONAL CREATED");

        PnCost c1 = new PnCost();
        c1.setDeliveryDriverCode("ABC-1l-123");
        c1.setUuid("ABC-UUID-342");
        c1.setTenderCode("TENDER-102");
        c1.setProductType("AR");
        c1.setCap(List.of("21047", "21050", "81022", "13000", "99999"));
        c1.setBasePrice(BigDecimal.valueOf(2.25));
        c1.setPagePrice(BigDecimal.valueOf(2.29));
        c1.setFsu(true);
        costDAO.createOrUpdate(c1).block();
        log.info("COST c1 CREATED");

        costInternationalFSU1.setDeliveryDriverCode("ABC-INTER");
        costInternationalFSU1.setUuid("ABC-UUID-INTER-FUS-AR-2");
        costInternationalFSU1.setTenderCode("TENDER-1");
        costInternationalFSU1.setProductType("AR");
        costInternationalFSU1.setZone("ZONE_1");
        costInternationalFSU1.setBasePrice(Utility.toBigDecimal("2.21"));
        costInternationalFSU1.setPagePrice(Utility.toBigDecimal("2.24"));
        costInternationalFSU1.setFsu(true);
        costDAO.createOrUpdate(costInternationalFSU1).block();
        log.info("COST INTERNATIONAL FSU 1 CREATED");


        costInternationalFSU2.setDeliveryDriverCode("ABC-INTER");
        costInternationalFSU2.setUuid("ABC-UUID-INTER-FSU-ar-1");
        costInternationalFSU2.setTenderCode("TENDER-1");
        costInternationalFSU2.setProductType("AR");
        costInternationalFSU2.setZone("ZONE_2");
        costInternationalFSU2.setBasePrice(BigDecimal.valueOf(2.29));
        costInternationalFSU2.setPagePrice(BigDecimal.valueOf(2.26));
        costInternationalFSU2.setFsu(true);
        costDAO.createOrUpdate(costInternationalFSU2).block();
        log.info("COST INTERNATIONAL FSU 2 CREATED");

    }

}
