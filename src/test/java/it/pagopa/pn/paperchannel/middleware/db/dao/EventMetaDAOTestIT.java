package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventMetaDAOTestIT extends BaseTest {

    @Autowired
    EventMetaDAO eventMetaDAO;

    private final PnEventMeta eventMeta1 = new PnEventMeta();
    private final PnEventMeta eventMeta2 = new PnEventMeta();

    @BeforeEach
    public void setUp(){
        initialize();
    }

    @Test
    void createPutGetUpdateDelete() {
        // put
        PnEventMeta insertedEventMeta = eventMetaDAO.createOrUpdate(eventMeta1).block();

        assertNotNull(insertedEventMeta);
        assertEquals(eventMeta1, insertedEventMeta);

        // get check
        PnEventMeta eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta1.getMetaRequestId(), eventMeta1.getMetaStatusCode()).block();

        assertNotNull(eventMetaFromDb);
        assertEquals(eventMeta1, eventMetaFromDb);

        // update
        final String otherDiscoveredAddress = "other discovered address";
        eventMeta1.setDiscoveredAddress(otherDiscoveredAddress);
        eventMetaDAO.createOrUpdate(eventMeta1).block();
        eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta1.getMetaRequestId(), eventMeta1.getMetaStatusCode()).block();

        assertNotNull(eventMetaFromDb);
        assertEquals(otherDiscoveredAddress, eventMetaFromDb.getDiscoveredAddress());
        assertEquals(eventMeta1, eventMetaFromDb);

        // delete
        PnEventMeta deletedEventMeta = eventMetaDAO.deleteEventMeta(eventMeta1.getMetaRequestId(), eventMeta1.getMetaStatusCode()).block();
        assertNotNull(deletedEventMeta);
        assertEquals(eventMetaFromDb, deletedEventMeta); // the previously modified one
        assertEquals(eventMeta1, deletedEventMeta);

        eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta1.getMetaRequestId(), eventMeta1.getMetaStatusCode()).block();
        assertNull(eventMetaFromDb);

        // delete missing
        deletedEventMeta = eventMetaDAO.deleteEventMeta("missing1", "missing2").block();
        assertNull(deletedEventMeta);
    }

    @Test
    void createMultipleFindAllDelete() {
        // create and findAll
        eventMetaDAO.createOrUpdate(eventMeta1).block();
        eventMetaDAO.createOrUpdate(eventMeta2).block();

        List<PnEventMeta> eventsFromDb = eventMetaDAO.findAllByRequestId(eventMeta1.getMetaRequestId()).collectList().block();

        assertNotNull(eventsFromDb);
        assertEquals(2, eventsFromDb.size());
        assertEquals(eventMeta1, eventsFromDb.get(0));
        assertEquals(eventMeta2, eventsFromDb.get(1));

        // delete and findAll
        eventMetaDAO.deleteEventMeta(eventsFromDb.get(0).getMetaRequestId(), eventsFromDb.get(0).getMetaStatusCode()).block();
        eventMetaDAO.deleteEventMeta(eventsFromDb.get(1).getMetaRequestId(), eventsFromDb.get(1).getMetaStatusCode()).block();

        eventsFromDb = eventMetaDAO.findAllByRequestId(eventMeta1.getMetaRequestId()).collectList().block();

        assertNotNull(eventsFromDb);
        assertEquals(0, eventsFromDb.size());
    }

    @Test
    void deleteBatchTest() {
        final String requestId = "LVRK-202302-G-1;RECINDEX_0;SENTATTEMPTMADE_0;PCRETRY_0";
        eventMeta1.setMetaRequestId("META##" + requestId);
        eventMeta1.setMetaStatusCode("META##RECAG011B");
        eventMeta1.setRequestId(requestId);
        eventMeta1.setStatusCode("RECAG011B");

        eventMeta2.setMetaRequestId("META##" + requestId);
        eventMeta2.setMetaStatusCode("META##PNAG012");
        eventMeta2.setRequestId(requestId);
        eventMeta2.setStatusCode("RECAG011B");

        eventMetaDAO.createOrUpdate(eventMeta1).block();
        eventMetaDAO.createOrUpdate(eventMeta2).block();

        List<PnEventMeta> result = eventMetaDAO.findAllByRequestId("META##" + requestId).collectList().block();

        assertThat(result).hasSize(2);

        eventMetaDAO.deleteBatch("META##" + requestId, "META##RECAG011B", "META##PNAG012").block();

        result = eventMetaDAO.findAllByRequestId("META##" + requestId).collectList().block();

        assertThat(result).isEmpty();

    }

    private void initialize() {
        final String requestId = "requestId";
        final String statusCode1 = "statusCode1";
        final String statusCode2 = "statusCode2";
        final int ttlOffsetDays = 365;
        final Instant now1 = Instant.now();
        final Instant now2 = Instant.now().plusSeconds(10);

        final String sameMetaRequestId = "META##" + requestId;

        eventMeta1.setMetaRequestId(sameMetaRequestId);
        eventMeta1.setMetaStatusCode("META##" + statusCode1);
        eventMeta1.setRequestId(requestId);
        eventMeta1.setStatusCode(statusCode1);
        eventMeta1.setDiscoveredAddress("discoveredAddress1");
        eventMeta1.setDeliveryFailureCause("failureCause1");
        eventMeta1.setStatusDateTime(now1);
        eventMeta1.setTtl(now1.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        eventMeta2.setMetaRequestId(sameMetaRequestId);
        eventMeta2.setMetaStatusCode("META##" + statusCode2);
        eventMeta2.setRequestId(requestId);
        eventMeta2.setStatusCode(statusCode1);
        eventMeta2.setDiscoveredAddress("discoveredAddress2");
        eventMeta2.setDeliveryFailureCause("failureCause2");
        eventMeta2.setStatusDateTime(now2);
        eventMeta2.setTtl(now2.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());
    }
}
