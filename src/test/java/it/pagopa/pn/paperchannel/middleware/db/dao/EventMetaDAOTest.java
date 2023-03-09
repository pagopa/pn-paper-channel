package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventMetaDAOTest extends BaseTest {
    private final String requestId = "requestId";
    private final String statusCode1 = "statusCode1";
    private final Instant now1 = Instant.now();
    private final String statusCode2 = "statusCode2";
    private final Instant now2 = Instant.now().plusSeconds(10);
    private final int ttlOffsetDays = 365;
    @Autowired
    private EventMetaDAO eventMetaDAO;

    private final PnEventMeta eventMeta1 = new PnEventMeta();
    private final PnEventMeta eventMeta2 = new PnEventMeta();

    @BeforeEach
    public void setUp(){
        initialize();
    }

    @Test
    void CreateGetDeleteIntegration() {
        eventMetaDAO.createOrUpdate(eventMeta1).block();

        PnEventMeta eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta1.getMetaRequestId(), eventMeta1.getMetaStatusCode()).block();

        assertNotNull(eventMetaFromDb);
        assertEquals(eventMeta1.getRequestId(), eventMetaFromDb.getRequestId());
        assertEquals(eventMeta1.getMetaStatusCode(), eventMetaFromDb.getMetaStatusCode());
        assertEquals(eventMeta1.getDiscoveredAddress(), eventMetaFromDb.getDiscoveredAddress());
        assertEquals(eventMeta1.getDeliveryFailureCause(), eventMetaFromDb.getDeliveryFailureCause());
        assertEquals(eventMeta1.getStatusDateTime(), eventMetaFromDb.getStatusDateTime());
        assertEquals(eventMeta1.getTtl(), eventMetaFromDb.getTtl());
    }

    private void initialize() {
        eventMeta1.setMetaRequestId("META##" + requestId);
        eventMeta1.setMetaStatusCode("META##" + statusCode1);
        eventMeta1.setRequestId(requestId);
        eventMeta1.setStatusCode(statusCode1);
        eventMeta1.setDiscoveredAddress("discoveredAddress1");
        eventMeta1.setDeliveryFailureCause("failureCause1");
        eventMeta1.setStatusDateTime(now1);
        eventMeta1.setTtl(now1.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        eventMeta2.setMetaRequestId("META##" + requestId);
        eventMeta2.setMetaStatusCode("META##" + statusCode2);
        eventMeta2.setRequestId(requestId);
        eventMeta2.setStatusCode(statusCode1);
        eventMeta2.setDiscoveredAddress("discoveredAddress2");
        eventMeta2.setDeliveryFailureCause("failureCause2");
        eventMeta2.setStatusDateTime(now2);
        eventMeta2.setTtl(now2.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());
    }
}
