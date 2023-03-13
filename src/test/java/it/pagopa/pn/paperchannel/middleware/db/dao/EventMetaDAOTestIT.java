package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventMetaDAOTestIT extends BaseTest {

    @Autowired
    EventMetaDAO eventMetaDAO;

    private final PnEventMeta eventMeta1 = new PnEventMeta();
    private final PnEventMeta eventMeta2 = new PnEventMeta();

    private final PnEventMeta eventMeta3 = new PnEventMeta();

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
        final PnDiscoveredAddress otherDiscoveredAddress = new PnDiscoveredAddress();
        otherDiscoveredAddress.setAddress("other discovered address");

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
        eventMetaDAO.createOrUpdate(eventMeta3).block();

        List<PnEventMeta> eventsFromDb = eventMetaDAO.findAllByRequestId(eventMeta1.getMetaRequestId()).collectList().block();

        // added 3 events, but 2 must be found
        assertNotNull(eventsFromDb);
        assertEquals(2, eventsFromDb.size());
        assertEquals(eventMeta1, eventsFromDb.get(0));
        assertEquals(eventMeta2, eventsFromDb.get(1));

        // get the missing event
        PnEventMeta eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta3.getMetaRequestId(), eventMeta3.getMetaStatusCode()).block();
        assertNotNull(eventMetaFromDb);
        assertEquals(eventMeta3, eventMetaFromDb);

        // delete and findAll
        eventMetaDAO.deleteEventMeta(eventsFromDb.get(0).getMetaRequestId(), eventsFromDb.get(0).getMetaStatusCode()).block();
        eventMetaDAO.deleteEventMeta(eventsFromDb.get(1).getMetaRequestId(), eventsFromDb.get(1).getMetaStatusCode()).block();
        eventMetaDAO.deleteEventMeta(eventMeta3.getMetaRequestId(), eventMeta3.getMetaStatusCode()).block();

        eventsFromDb = eventMetaDAO.findAllByRequestId(eventMeta1.getMetaRequestId()).collectList().block();

        assertNotNull(eventsFromDb);
        assertEquals(0, eventsFromDb.size());

        eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta3.getMetaRequestId(), eventMeta3.getMetaStatusCode()).block();
        assertNull(eventMetaFromDb);
    }

    private void initialize() {
        final String requestId = "requestId";
        final String statusCode1 = "statusCode1";
        final String statusCode2 = "statusCode2";
        final String statusCode3 = "statusCode3";
        final int ttlOffsetDays = 365;
        final Instant now1 = Instant.now();
        final Instant now2 = Instant.now().plusSeconds(10);

        final PnDiscoveredAddress address1 = new PnDiscoveredAddress();
        address1.setAddress("discoveredAddress1");
        final PnDiscoveredAddress address2 = new PnDiscoveredAddress();
        address2.setAddress("discoveredAddress2");
        final PnDiscoveredAddress address3 = new PnDiscoveredAddress();
        address3.setAddress("discoveredAddress3");

        final String sameMetaRequestId = "META##" + requestId;

        eventMeta1.setMetaRequestId(sameMetaRequestId);
        eventMeta1.setMetaStatusCode("META##" + statusCode1);
        eventMeta1.setRequestId(requestId);
        eventMeta1.setStatusCode(statusCode1);
        eventMeta1.setDiscoveredAddress(address1);
        eventMeta1.setDeliveryFailureCause("failureCause1");
        eventMeta1.setStatusDateTime(now1);
        eventMeta1.setTtl(now1.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        eventMeta2.setMetaRequestId(sameMetaRequestId);
        eventMeta2.setMetaStatusCode("META##" + statusCode2);
        eventMeta2.setRequestId(requestId);
        eventMeta2.setStatusCode(statusCode1);
        eventMeta2.setDiscoveredAddress(address2);
        eventMeta2.setDeliveryFailureCause("failureCause2");
        eventMeta2.setStatusDateTime(now2);
        eventMeta2.setTtl(now2.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        eventMeta3.setMetaRequestId("differentRequestId");
        eventMeta3.setMetaStatusCode("META##" + statusCode3);
        eventMeta3.setRequestId(requestId);
        eventMeta3.setStatusCode(statusCode3);
        eventMeta3.setDiscoveredAddress(address3);
        eventMeta3.setDeliveryFailureCause("failureCause3");
        eventMeta3.setStatusDateTime(now2);
        eventMeta3.setTtl(now2.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());
    }
}
