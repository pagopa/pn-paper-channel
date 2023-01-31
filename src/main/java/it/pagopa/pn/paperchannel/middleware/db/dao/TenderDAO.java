package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperTender;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TenderDAO {
    Mono<List<PnPaperTender>> getTenders();
}
