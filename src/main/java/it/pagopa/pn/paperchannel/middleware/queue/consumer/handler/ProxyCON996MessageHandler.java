package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@SuperBuilder
@Slf4j
public class ProxyCON996MessageHandler implements MessageHandler {

    private final RetryableErrorMessageHandler retryableErrorMessageHandler;
    private final NotRetryableErrorMessageHandler notRetryableErrorMessageHandler;
    private final RequestDeliveryDAO requestDeliveryDAO;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        if (Boolean.TRUE.equals(entity.getApplyRasterization())) {
            log.info("Proxying message to NotRetryableErrorMessageHandler");
            return notRetryableErrorMessageHandler.handleMessage(entity, paperRequest);
        } else {
            entity.setApplyRasterization(Boolean.TRUE);
            return requestDeliveryDAO.updateApplyRasterization(entity.getRequestId(), entity.getApplyRasterization())
                    .thenReturn(entity)
                    .doOnNext(updatedEntity -> log.info("Proxying message to RetryableErrorMessageHandler"))
                    .flatMap(updatedEntity -> retryableErrorMessageHandler.handleMessage(updatedEntity, paperRequest));
        }
    }
}
