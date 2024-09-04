package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelTender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;


class PnPaperTenderDAOTestIT extends BaseTest {
    private static final String TENDER_ID = "GARA_2024";
    private static final Instant ACTIVATION_DATA = Instant.now().minusSeconds(3600);


    @Autowired
    private PnPaperTenderDAO pnPaperTenderDAO;

    @BeforeEach
    public void setUp() {
        // Arrange: Preparazione dei dati di test
        arrangeDB();
    }

    @Test
    void testGetActiveTender() {
        // Act: Azione - Recupero del tender attivo
        Mono<PnPaperChannelTender> result = pnPaperTenderDAO.getActiveTender();

        // Assert: Verifica - Controllare che il tender attivo sia stato recuperato correttamente
        StepVerifier.create(result)
                .assertNext(tender -> {
                    assertNotNull(tender);
                    assertTrue(tender.getActivationDate().isBefore(Instant.now()));
                    assertEquals(ACTIVATION_DATA, tender.getActivationDate());
                })
                .verifyComplete();
    }

    @Test
    void getTenderById() {
        // Act: Azione - Recupero del tender
        Mono<PnPaperChannelTender> result = pnPaperTenderDAO.getTenderById(TENDER_ID);

        // Assert: Verifica - Controllare che il tender sia stato recuperato correttamente
        StepVerifier.create(result)
                .assertNext(tender -> {
                    assertNotNull(tender);
                    assertTrue(tender.getActivationDate().isBefore(Instant.now()));
                    assertEquals(ACTIVATION_DATA, tender.getActivationDate());
                })
                .verifyComplete();
    }

    private void arrangeDB() {
        var testArrange = new PnPaperChannelTender();
        testArrange.setTenderId(TENDER_ID);
        testArrange.setActivationDate(ACTIVATION_DATA); // Un ora fa
        pnPaperTenderDAO.createOrUpdate(testArrange).block();


        testArrange = new PnPaperChannelTender();
        testArrange.setTenderId(TENDER_ID);
        testArrange.setActivationDate(Instant.now().minusSeconds(3600*24)); // Un giorno fa
        pnPaperTenderDAO.createOrUpdate(testArrange).block();

        testArrange = new PnPaperChannelTender();
        testArrange.setTenderId(TENDER_ID);
        testArrange.setActivationDate(Instant.now().minusSeconds(3600*21)); // 21 ora fa
        pnPaperTenderDAO.createOrUpdate(testArrange).block();

        testArrange = new PnPaperChannelTender();
        testArrange.setTenderId(TENDER_ID);
        testArrange.setActivationDate(Instant.now().minusSeconds(3600*14)); // 14 ora fa
        pnPaperTenderDAO.createOrUpdate(testArrange).block();
    }
}