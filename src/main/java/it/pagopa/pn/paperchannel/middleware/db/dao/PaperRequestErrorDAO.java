package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InfoDownloadDTO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaperRequestErrorDAO {


    Mono<PnRequestError> created(String requestId, String error, String classType);

    Mono<List<PnRequestError>>  findAll();
}
