package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperRequestError;
import reactor.core.publisher.Mono;

public interface PaperRequestErrorDAO {


    Mono<PnPaperRequestError> created(String requestId, String error, String classType);

}
