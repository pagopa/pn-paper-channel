package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CapDAOTest extends BaseTest {
    @Autowired
    private CapDAO capDAO;


    @Test
    void getAllCapSuccess() {
        String capTo = "35031";
        List<PnCap> monoCapList =  capDAO.getAllCap(capTo).block();
        assertNotNull(monoCapList);
        assertEquals(1, monoCapList.size());
        monoCapList.forEach(cap -> {assertEquals(cap.getCap(), capTo);
        assertEquals(cap.getCity(), "Abano Terme");});
    }

    @Test
    void getAllCapSuccessWithEmptyCap() {
        String cap = "";
        Mono<List<PnCap>> monoCapList =  capDAO.getAllCap(cap);
        assertNotNull(monoCapList);
        assertTrue(monoCapList
                .block()
                .size() > 0);
    }
}
