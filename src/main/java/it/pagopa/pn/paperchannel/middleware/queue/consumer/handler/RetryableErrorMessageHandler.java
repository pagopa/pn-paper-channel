package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_API_EXCEPTION;

// handler per stati gialli: Retry su ExtCh con suffisso +1, invio dello stato in progress verso DP
@Slf4j
@SuperBuilder
public class RetryableErrorMessageHandler extends SendToDeliveryPushHandler {

    private final PaperRequestErrorDAO paperRequestErrorDAO;
    private final PcRetryUtils pcRetryUtils;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        if (pcRetryUtils.hasOtherAttempt(paperRequest.getRequestId())) {
            //invio di nuovo la richiesta a ext-channels
            return pcRetryUtils.sendEngageRequest(entity, pcRetryUtils.setRetryRequestId(paperRequest.getRequestId()))
                    .flatMap(pnDeliveryRequest -> super.handleMessage(entity, paperRequest));
        } else {

            PnRequestError pnRequestError = PnRequestError.builder()
                    .requestId(entity.getRequestId())
                    .paId(entity.getRequestPaId())
                    .error(EXTERNAL_CHANNEL_API_EXCEPTION.getMessage())
                    .flowThrow(EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                    .build();

            return paperRequestErrorDAO
                    .created(pnRequestError)
                    .flatMap(requestError -> super.handleMessage(entity, paperRequest));
        }

    }

}
