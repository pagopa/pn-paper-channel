package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventDematDAOTestIT extends BaseTest {
    @Autowired
    EventDematDAO eventDematDAO;

    private final PnEventDemat eventDemat1 = new PnEventDemat();
    private final PnEventDemat eventDemat2 = new PnEventDemat();

    @BeforeEach
    public void setUp(){
        initialize();
    }

    @Test
    void createPutGetUpdateDelete() {
        PnEventDemat insertedEventDemat = eventDematDAO.createOrUpdate(eventDemat1).block();

        assertNotNull(insertedEventDemat);
        assertEquals(eventDemat1, insertedEventDemat);

        // get check
        PnEventDemat eventDematFromDb = eventDematDAO.getDeliveryEventDemat(eventDemat1.getDematRequestId(), eventDemat1.getDocumentTypeStatusCode()).block();

        assertNotNull(eventDematFromDb);
        assertEquals(eventDemat1, eventDematFromDb);

        // update
        final String otherUri = "otherUri";
        eventDemat1.setUri(otherUri);
        eventDematDAO.createOrUpdate(eventDemat1).block();
        eventDematFromDb = eventDematDAO.getDeliveryEventDemat(eventDemat1.getDematRequestId(), eventDemat1.getDocumentTypeStatusCode()).block();

        assertNotNull(eventDematFromDb);
        assertEquals(otherUri, eventDematFromDb.getUri());
        assertEquals(eventDemat1, eventDematFromDb);

        // delete
        PnEventDemat deletedEventDemat = eventDematDAO.deleteEventDemat(eventDemat1.getDematRequestId(), eventDemat1.getDocumentTypeStatusCode()).block();
        assertNotNull(deletedEventDemat);
        assertEquals(eventDematFromDb, deletedEventDemat); // the previously modified one
        assertEquals(eventDemat1, deletedEventDemat);

        eventDematFromDb = eventDematDAO.getDeliveryEventDemat(eventDemat1.getDematRequestId(), eventDemat1.getDocumentTypeStatusCode()).block();
        assertNull(eventDematFromDb);

        // delete missing
        deletedEventDemat = eventDematDAO.deleteEventDemat("missing1", "missing2").block();
        assertNull(deletedEventDemat);
    }

    @Test
    void createMultipleFindAllDelete() {
        eventDematDAO.createOrUpdate(eventDemat1).block();
        eventDematDAO.createOrUpdate(eventDemat2).block();

        List<PnEventDemat> eventsFromDb = eventDematDAO.findAllByRequestId(eventDemat1.getDematRequestId()).collectList().block();

        assertNotNull(eventsFromDb);
        assertEquals(2, eventsFromDb.size());
        assertEquals(eventDemat1, eventsFromDb.get(0));
        assertEquals(eventDemat2, eventsFromDb.get(1));

        // delete and findAll
        eventDematDAO.deleteEventDemat(eventDemat1.getDematRequestId(), eventDemat1.getDocumentTypeStatusCode()).block();
        eventDematDAO.deleteEventDemat(eventDemat2.getDematRequestId(), eventDemat2.getDocumentTypeStatusCode()).block();

        eventsFromDb = eventDematDAO.findAllByRequestId(eventDemat1.getDematRequestId()).collectList().block();

        assertNotNull(eventsFromDb);
        assertEquals(0, eventsFromDb.size());
    }

    private void initialize() {
        final String requestId = "requestId";
        final String statusCode1 = "statusCode1";
        final String statusCode2 = "statusCode2";
        final String documentType1 = "23L";
        final String documentType2 = "AR";
        final int ttlOffsetDays = 365;
        final Instant now1 = Instant.now();
        final Instant now2 = Instant.now().plusSeconds(10);

        final String sameDematRequestId = "DEMAT##" + requestId;

        eventDemat1.setDematRequestId(sameDematRequestId);
        eventDemat1.setDocumentTypeStatusCode(documentType1 + "##" + statusCode1);
        eventDemat1.setRequestId(requestId);
        eventDemat1.setDocumentType(documentType1);
        eventDemat1.setStatusCode(statusCode1);
        eventDemat1.setDocumentDate(now1.plusSeconds(-5));
        eventDemat1.setStatusDateTime(now1);
        eventDemat1.setUri("uri1");
        eventDemat1.setTtl(now1.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        eventDemat2.setDematRequestId(sameDematRequestId);
        eventDemat2.setDocumentTypeStatusCode(documentType2 + "##" + statusCode2);
        eventDemat2.setRequestId(requestId);
        eventDemat2.setDocumentType(documentType2);
        eventDemat2.setStatusCode(statusCode2);
        eventDemat2.setDocumentDate(now2.plusSeconds(-10));
        eventDemat2.setStatusDateTime(now2);
        eventDemat2.setUri("uri2");
        eventDemat2.setTtl(now2.plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());
    }
}
