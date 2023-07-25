package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COUNTRY_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ZoneDAOTestIT extends BaseTest {
    @Autowired
    private ZoneDAO zoneDAO;

    private final PnZone zoneCountry = new PnZone();

    @BeforeEach
    public void setUp(){
        initialValue();
    }

    @Test
    void getByCountryTest(){
        StepVerifier.create(this.zoneDAO.getByCountry(zoneCountry.getCountryEn()))
                .expectErrorMatches(ex -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(COUNTRY_NOT_FOUND, ((PnGenericException) ex).getExceptionType());
                    return true;
                });
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