package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COUNTRY_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ZoneDAOTest extends BaseTest {
    @Autowired
    private ZoneDAO zoneDAO;

    private final PnZone zoneCountry = new PnZone();

    @BeforeEach
    public void setUp(){
        initialValue();
    }

    @Test
    void getByCountryTest(){
        PnZone zone = this.zoneDAO.getByCountry(zoneCountry.getCountryEn()).block();
        assertNotNull(zone);
        assertEquals("countryIt",zone.getCountryIt());
        assertEquals("countryEn",zone.getCountryEn());
        assertEquals("zone_1",zone.getZone());
    }

    @Test
    void getByCountryErrorTest(){
        //StepVerifier.create(this.zoneDAO.getByCountry("test-zone-error")).expectError(PnGenericException.class).verify();
        this.zoneDAO.getByCountry("test-zone-error").onErrorResume(ex -> {
            assertEquals(ex.getMessage(),COUNTRY_NOT_FOUND.getMessage());
            return Mono.empty();
        }).block();
    }

    private void initialValue() {
        zoneCountry.setZone("zone");
        zoneCountry.setCountryIt("countryIt");
        zoneCountry.setCountryEn("countryEn");
    }

}