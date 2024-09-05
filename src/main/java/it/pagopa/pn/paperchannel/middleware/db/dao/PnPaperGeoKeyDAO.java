package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelGeoKey;
import reactor.core.publisher.Mono;

public interface PnPaperGeoKeyDAO {


    Mono<PnPaperChannelGeoKey> createOrUpdate(PnPaperChannelGeoKey paperChannelGeoKey);

    Mono<PnPaperChannelGeoKey> getGeoKey(String tenderId, String product, String geoKey);


}
