package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelGeoKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PnPaperGeoKeyDAOTestIT extends BaseTest {
    private static final String TENDER_ID = "GARA_2024";
    private static final String PRODUCT = "AR";
    private static final String GEO_KEY = "20344";
    @Autowired
    private PnPaperGeoKeyDAO pnPaperGeoKeyDAO;



    @BeforeEach
    public void setUp() {
        // Arrange: Preparazione dei dati di test
        arrangeDB();
    }

    @Test
    void testRetrieveActiveGeokeyWithDismissedFalse() {

        // Act: Recupero del GeoKey dal database
        Mono<PnPaperChannelGeoKey> result = pnPaperGeoKeyDAO.getGeoKey(TENDER_ID, PRODUCT, GEO_KEY);

        // Assert: Verifica che il GeoKey sia stato recuperato correttamente
        StepVerifier.create(result)
                .assertNext(geoKey -> {
                    assertNotNull(geoKey);
                    assertEquals(TENDER_ID, geoKey.getTenderId());
                    assertEquals(PRODUCT, geoKey.getProduct());
                    assertEquals(GEO_KEY, geoKey.getGeokey());
                    assertTrue(geoKey.getActivationDate().isBefore(Instant.now()));
                    assertEquals(false, geoKey.getDismissed());
                })
                .verifyComplete();
    }

    @Test
    void testRetrieveActiveGeokeyWithDismissedTrue() {
        // Arrange
        var geoKeyDismissed = new PnPaperChannelGeoKey(TENDER_ID, PRODUCT, GEO_KEY);
        geoKeyDismissed.setActivationDate(Instant.now().minusSeconds(300));
        geoKeyDismissed.setDismissed(true);
        geoKeyDismissed.setLot("LOT 1");
        geoKeyDismissed.setZone("ZONE 1");

        // add Recently Dismissed GeoKey
        pnPaperGeoKeyDAO.createOrUpdate(geoKeyDismissed).block();

        // Act: Recupero del GeoKey dal database AND Assert: Verifica che il GeoKey sia stato recuperato correttamente
        StepVerifier.create(pnPaperGeoKeyDAO.getGeoKey(TENDER_ID, PRODUCT, GEO_KEY))
                .expectErrorMatches(error -> {
                    assertNotNull(error);
                    assertInstanceOf(PnGenericException.class, error);
                    var exception = (PnGenericException) error;
                    assertEquals(exception.getExceptionType(), ExceptionTypeEnum.GEOKEY_NOT_FOUND);
                    return true;
                }).verify();
    }


    private void arrangeDB() {

        var testArrange = new PnPaperChannelGeoKey(TENDER_ID, PRODUCT, GEO_KEY);
        testArrange.setActivationDate(Instant.now().minusSeconds(3600*48)); // Due giorni fa
        testArrange.setDismissed(true);
        testArrange.setLot("LOT 1");
        testArrange.setZone("ZONE 1");

        // add dismissed GeoKey
        pnPaperGeoKeyDAO.createOrUpdate(testArrange).block();

        testArrange = new PnPaperChannelGeoKey(TENDER_ID, PRODUCT, GEO_KEY);
        testArrange.setActivationDate(Instant.now().minusSeconds(3600*24)); // Un giorni fa
        testArrange.setDismissed(true);
        testArrange.setLot("LOT 1");
        testArrange.setZone("ZONE 1");

        // add dismissed GeoKey
        pnPaperGeoKeyDAO.createOrUpdate(testArrange).block();

        testArrange = new PnPaperChannelGeoKey(TENDER_ID, PRODUCT, GEO_KEY);
        testArrange.setActivationDate(Instant.now().minusSeconds(3600*34)); // 34 ore fa fa
        testArrange.setDismissed(false);
        testArrange.setLot("LOT 1");
        testArrange.setZone("ZONE 1");

        // add not Dismissed GeoKey
        pnPaperGeoKeyDAO.createOrUpdate(testArrange).block();

        testArrange = new PnPaperChannelGeoKey(TENDER_ID, PRODUCT, GEO_KEY);
        testArrange.setActivationDate(Instant.now().minusSeconds(3600*18)); // 18 ore fa fa
        testArrange.setDismissed(false);
        testArrange.setLot("LOT 1");
        testArrange.setZone("ZONE 1");

        // add not Dismissed GeoKey
        pnPaperGeoKeyDAO.createOrUpdate(testArrange).block();

    }

}
