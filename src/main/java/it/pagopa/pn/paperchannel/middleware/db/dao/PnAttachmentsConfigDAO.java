package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface PnAttachmentsConfigDAO {

    Mono<PnAttachmentsConfig> findConfigInInterval(String configKey, Instant dateValidity);

    Mono<PnAttachmentsConfig> putItem(PnAttachmentsConfig pnAttachmentsConfig);

    Mono<Void> putItemInTransaction(String configKey, List<PnAttachmentsConfig> pnAttachmentsConfigs);

    Flux<PnAttachmentsConfig> findAllByConfigKey(String configKey);

}
