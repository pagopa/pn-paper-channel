package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelCost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


class PnPaperCostDAOTestIT extends BaseTest {
    private static final String TENDER_ID = "GARA_2024";
    private static final String PRODUCT = "AR";
    private static final String LOT = "LOT_1";
    private static final String ZONE = "ZONE_1";

    @Autowired
    private PnPaperCostDAO pnPaperCostDAO;


    @BeforeEach
    public void setUp() {
        // Arrange: Preparazione dei dati di test
        arrangeDB();
    }


    @Test
    void testGetActiveTender() {
        // Act: Recupero del PaperCost dal database
        Mono<PnPaperChannelCost> result = pnPaperCostDAO.getCostFrom(TENDER_ID, PRODUCT, LOT, ZONE);

        // Assert: Verifica che il GeoKey sia stato recuperato correttamente
        StepVerifier.create(result)
                .assertNext(cost -> {
                    assertNotNull(cost);
                    assertEquals(TENDER_ID, cost.getTenderId());
                    assertEquals(PRODUCT, cost.getProduct());
                    assertEquals(LOT, cost.getLot());
                    assertEquals(ZONE, cost.getZone());
                })
                .verifyComplete();

    }

    private void arrangeDB() {
        var cost = new PnPaperChannelCost(TENDER_ID, PRODUCT, LOT, ZONE);
        pnPaperCostDAO.createOrUpdate(cost).block();

        cost = new PnPaperChannelCost(TENDER_ID, PRODUCT, LOT, "1234");
        pnPaperCostDAO.createOrUpdate(cost).block();

        cost = new PnPaperChannelCost(TENDER_ID, PRODUCT, "LOT", "1234");
        pnPaperCostDAO.createOrUpdate(cost).block();

        cost = new PnPaperChannelCost(TENDER_ID, "890", "LOT", "1234");
        pnPaperCostDAO.createOrUpdate(cost).block();
    }
}
