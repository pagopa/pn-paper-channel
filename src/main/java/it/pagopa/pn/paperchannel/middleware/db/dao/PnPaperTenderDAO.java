package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelTender;
import reactor.core.publisher.Mono;

public interface PnPaperTenderDAO {

    Mono<PnPaperChannelTender> createOrUpdate(PnPaperChannelTender pnTender);


    Mono<PnPaperChannelTender> getActiveTender();

}
