package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.model.RaddSearchMode;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface RaddAltService {
    Mono<Boolean> isAreaCovered(RaddSearchMode searchMode, PnAddress pnAddress, Instant searchDate);
}
