package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildDematRequestId;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaRequestId;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetaDematCleaner {

    private final EventDematDAO eventDematDAO;

    private final EventMetaDAO eventMetaDAO;

    public Mono<Void> clean(String requestId) {
        String pkMetaFilter = buildMetaRequestId(requestId);
        String pkDematFilter = buildDematRequestId(requestId);

        return eventMetaDAO.findAllByRequestId(pkMetaFilter).collectList()
                .map(this::mapMetasToSortKeys)
                .flatMap(sortKeysMeta -> {
                    if(sortKeysMeta.length > 0) return eventMetaDAO.deleteBatch(pkMetaFilter, sortKeysMeta);
                    return Mono.empty();
                })
                .onErrorResume(throwable -> {
                    log.warn("Error in clean Metadata", throwable);
                    return Mono.empty();
                })
                .then(eventDematDAO.findAllByRequestId(pkDematFilter).collectList())
                .map(this::mapDematsToSortKeys)
                .flatMap(sortKeysDemat -> {
                    if(sortKeysDemat.length > 0) return eventDematDAO.deleteBatch(pkDematFilter, sortKeysDemat);
                    return Mono.empty();
                })
                .onErrorResume(throwable -> {
                    log.warn("Error in clean Demat", throwable);
                    return Mono.empty();
                });

    }

    private String[] mapMetasToSortKeys(List<PnEventMeta> pnEventMetas) {
        return pnEventMetas.stream().map(PnEventMeta::getMetaStatusCode).toArray(String[]::new);
    }

    private String[] mapDematsToSortKeys(List<PnEventDemat> pnEventDemats) {
        return pnEventDemats.stream().map(PnEventDemat::getDocumentTypeStatusCode).toArray(String[]::new);
    }

}
