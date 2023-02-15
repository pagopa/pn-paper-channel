package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import reactor.core.publisher.Mono;

public interface FileDownloadDAO {

    Mono<PnDeliveryFile> getUuid(String uuid);
    Mono<PnDeliveryFile> create(PnDeliveryFile pnDeliveryFile);

}
