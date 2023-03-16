package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.utils.MetaDematUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class MetaDematCleanerTest {

    private MetaDematCleaner metaDematCleaner;

    private EventDematDAO eventDematDAO;

    private EventMetaDAO eventMetaDAO;

    @BeforeEach
    public void init() {
        eventDematDAO = mock(EventDematDAO.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        metaDematCleaner = new MetaDematCleaner(eventDematDAO, eventMetaDAO);

    }


    @Test
    void cleanTestOK() {
        String requestId = "requestId";
        String statusCode = "RECRS002A";
        String documentType = "23L";
        String metaRequestId = MetaDematUtils.buildMetaRequestId(requestId);
        String dematRequestId = MetaDematUtils.buildDematRequestId(requestId);
        String metaStatusCode = MetaDematUtils.buildMetaStatusCode(statusCode);
        String documentTypeStatusCode = MetaDematUtils.buildDocumentTypeStatusCode(documentType, statusCode);

        PnEventMeta eventMeta = new PnEventMeta();
        eventMeta.setMetaRequestId(metaRequestId);
        eventMeta.setRequestId(requestId);
        eventMeta.setStatusCode(statusCode);
        eventMeta.setMetaStatusCode(metaStatusCode);
        eventMeta.setRequestId(requestId);

        PnEventDemat eventDemat = new PnEventDemat();
        eventDemat.setDematRequestId(dematRequestId);
        eventDemat.setRequestId(requestId);
        eventDemat.setDocumentTypeStatusCode(documentTypeStatusCode);
        eventDemat.setDocumentType(documentType);

        when(eventMetaDAO.findAllByRequestId(metaRequestId)).thenReturn(Flux.just(eventMeta));
        when(eventMetaDAO.deleteBatch(metaRequestId, metaStatusCode)).thenReturn(Mono.empty());

        when(eventDematDAO.findAllByRequestId(dematRequestId)).thenReturn(Flux.just(eventDemat));
        when(eventDematDAO.deleteBatch(dematRequestId, documentTypeStatusCode)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> metaDematCleaner.clean(requestId).block());

        verify(eventMetaDAO, times(1)).findAllByRequestId(metaRequestId);
        verify(eventMetaDAO, times(1)).deleteBatch(metaRequestId, metaStatusCode);

        verify(eventDematDAO, times(1)).findAllByRequestId(dematRequestId);
        verify(eventDematDAO, times(1)).deleteBatch(dematRequestId, documentTypeStatusCode);

    }

    @Test
    void cleanTestDeleteMetaKO() {
        String requestId = "requestId";
        String statusCode = "RECRS002A";
        String documentType = "23L";
        String metaRequestId = MetaDematUtils.buildMetaRequestId(requestId);
        String dematRequestId = MetaDematUtils.buildDematRequestId(requestId);
        String metaStatusCode = MetaDematUtils.buildMetaStatusCode(statusCode);
        String documentTypeStatusCode = MetaDematUtils.buildDocumentTypeStatusCode(documentType, statusCode);

        PnEventMeta eventMeta = new PnEventMeta();
        eventMeta.setMetaRequestId(metaRequestId);
        eventMeta.setRequestId(requestId);
        eventMeta.setStatusCode(statusCode);
        eventMeta.setMetaStatusCode(metaStatusCode);
        eventMeta.setRequestId(requestId);

        PnEventDemat eventDemat = new PnEventDemat();
        eventDemat.setDematRequestId(dematRequestId);
        eventDemat.setRequestId(requestId);
        eventDemat.setDocumentTypeStatusCode(documentTypeStatusCode);
        eventDemat.setDocumentType(documentType);

        when(eventMetaDAO.findAllByRequestId(metaRequestId)).thenReturn(Flux.just(eventMeta));
        when(eventMetaDAO.deleteBatch(metaRequestId, metaStatusCode)).thenReturn(Mono.error(new RuntimeException()));

        when(eventDematDAO.findAllByRequestId(dematRequestId)).thenReturn(Flux.just(eventDemat));
        when(eventDematDAO.deleteBatch(dematRequestId, documentTypeStatusCode)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> metaDematCleaner.clean(requestId).block());

        verify(eventMetaDAO, times(1)).findAllByRequestId(metaRequestId);
        verify(eventMetaDAO, times(1)).deleteBatch(metaRequestId, metaStatusCode);

        verify(eventDematDAO, times(1)).findAllByRequestId(dematRequestId);
        verify(eventDematDAO, times(1)).deleteBatch(dematRequestId, documentTypeStatusCode);

    }

    @Test
    void cleanTestDeleteDematKO() {
        String requestId = "requestId";
        String statusCode = "RECRS002A";
        String documentType = "23L";
        String metaRequestId = MetaDematUtils.buildMetaRequestId(requestId);
        String dematRequestId = MetaDematUtils.buildDematRequestId(requestId);
        String metaStatusCode = MetaDematUtils.buildMetaStatusCode(statusCode);
        String documentTypeStatusCode = MetaDematUtils.buildDocumentTypeStatusCode(documentType, statusCode);

        PnEventMeta eventMeta = new PnEventMeta();
        eventMeta.setMetaRequestId(metaRequestId);
        eventMeta.setRequestId(requestId);
        eventMeta.setStatusCode(statusCode);
        eventMeta.setMetaStatusCode(metaStatusCode);
        eventMeta.setRequestId(requestId);

        PnEventDemat eventDemat = new PnEventDemat();
        eventDemat.setDematRequestId(dematRequestId);
        eventDemat.setRequestId(requestId);
        eventDemat.setDocumentTypeStatusCode(documentTypeStatusCode);
        eventDemat.setDocumentType(documentType);

        when(eventMetaDAO.findAllByRequestId(metaRequestId)).thenReturn(Flux.just(eventMeta));
        when(eventMetaDAO.deleteBatch(metaRequestId, metaStatusCode)).thenReturn(Mono.empty());

        when(eventDematDAO.findAllByRequestId(dematRequestId)).thenReturn(Flux.just(eventDemat));
        when(eventDematDAO.deleteBatch(dematRequestId, documentTypeStatusCode)).thenReturn(Mono.error(new RuntimeException()));

        assertDoesNotThrow(() -> metaDematCleaner.clean(requestId).block());

        verify(eventMetaDAO, times(1)).findAllByRequestId(metaRequestId);
        verify(eventMetaDAO, times(1)).deleteBatch(metaRequestId, metaStatusCode);

        verify(eventDematDAO, times(1)).findAllByRequestId(dematRequestId);
        verify(eventDematDAO, times(1)).deleteBatch(dematRequestId, documentTypeStatusCode);

    }
}
