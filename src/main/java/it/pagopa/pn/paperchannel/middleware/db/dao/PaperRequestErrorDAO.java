package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaperRequestErrorDAO {


    Mono<PnRequestError> created(PnRequestError pnRequestError);

    Mono<List<PnRequestError>>  findAll();
}
