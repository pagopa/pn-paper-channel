package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class CostDAOTest extends BaseTest {

    @Autowired
    private CostDAO costDAO;
    private final PnCost costNational = new PnCost();
    private final PnCost costInternational = new PnCost();

    @BeforeEach
    public void setUp(){
        initialValue();
    }

    @Test
    void retrieveCostByCapAndProductTypeTest(){
        PnCost costFinded = this.costDAO.getByCapOrZoneAndProductType(costNational.getTenderCode(), "21050", null, costNational.getProductType()).block();
        assertNotNull(costFinded);
        assertEquals(costNational.getTenderCode(), costFinded.getTenderCode());
        assertEquals(costNational.getDeliveryDriverCode(), costFinded.getDeliveryDriverCode());
        Assertions.assertNull(costFinded.getZone());

        StepVerifier.create(this.costDAO.getByCapOrZoneAndProductType(costNational.getTenderCode(), "01111", null, "890"))
                .expectComplete().verify();

        costFinded = this.costDAO.getByCapOrZoneAndProductType(
                costInternational.getTenderCode(),
                null,
                costInternational.getZone(),
                costInternational.getProductType()
        ).block();

        assertNotNull(costFinded);
        assertEquals(costInternational.getTenderCode(), costFinded.getTenderCode());
        assertEquals(costInternational.getDeliveryDriverCode(), costFinded.getDeliveryDriverCode());
        Assertions.assertNull(costFinded.getCap());
    }

    @Test
    void findAllFromTenderCodeTest(){
        List<PnCost> prices = this.costDAO.findAllFromTenderCode(costNational.getTenderCode()).block();
        assertNotNull(prices);
        assertEquals(2, prices.size());

        prices = this.costDAO.findAllFromTenderCode("TEST_ERROR_TENDER_CODE").block();
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
        assertEquals(2, costs.size());
        costs = this.costDAO
                .findAllFromTenderAndProductTypeAndExcludedUUID(
                        costNational.getTenderCode(),
                        costNational.getProductType(),
                        costNational.getUuid()
                ).block();
        assertNotNull(costs);
        assertEquals(1, costs.size());
    }


    private void initialValue(){
        costNational.setDeliveryDriverCode("ABC-1");
        costNational.setUuid("ABC-UUID");
        costNational.setTenderCode("TENDER-1");
        costNational.setProductType("AR");
        costNational.setCap(List.of("21047", "21050", "81022", "13000"));
        costNational.setBasePrice(2.2F);
        costNational.setPagePrice(2.2F);
        costDAO.createOrUpdate(costNational).block();

        PnCost c1 = new PnCost();
        c1.setDeliveryDriverCode("ABC-1l-123");
        c1.setUuid("ABC-UUID-342");
        c1.setTenderCode("TENDER-102");
        c1.setProductType("AR");
        c1.setCap(List.of("21047", "21050", "81022", "13000"));
        c1.setBasePrice(2.2F);
        c1.setPagePrice(2.2F);
        costDAO.createOrUpdate(c1).block();

        costInternational.setDeliveryDriverCode("ABC-INTER");
        costInternational.setUuid("ABC-UUID-INTER");
        costInternational.setTenderCode("TENDER-1");
        costInternational.setProductType("AR");
        costInternational.setZone("ZONE_1");
        costInternational.setBasePrice(2.2F);
        costInternational.setPagePrice(2.2F);
        costDAO.createOrUpdate(costInternational).block();

    }

}
