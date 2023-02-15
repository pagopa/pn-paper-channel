package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import reactor.core.publisher.Mono;

public interface PaperRequestErrorDAO {


    Mono<PnRequestError> created(String requestId, String error, String classType);

}
