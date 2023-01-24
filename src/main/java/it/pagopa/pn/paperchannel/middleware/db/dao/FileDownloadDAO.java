package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnFile;
import reactor.core.publisher.Mono;

public interface FileDownloadDAO {

    Mono<PnFile> getUuid(String uuid);
    Mono<PnFile> create(PnFile pnFile);

}
