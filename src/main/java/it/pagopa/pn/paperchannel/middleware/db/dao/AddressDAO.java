package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.AddressEntity;
import reactor.core.publisher.Mono;

public interface AddressDAO {
    Mono<AddressEntity> create (AddressEntity addressEntity);
}
