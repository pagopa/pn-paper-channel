package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import reactor.core.publisher.Mono;

public interface PaperResultAsyncService {
    Mono<PnDeliveryRequest> resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt);
}
