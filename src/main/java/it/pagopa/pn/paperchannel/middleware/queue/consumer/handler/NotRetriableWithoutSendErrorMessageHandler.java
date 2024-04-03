package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.Builder;
import reactor.core.publisher.Mono;

@Builder
public class NotRetriableWithoutSendErrorMessageHandler implements MessageHandler {

    private static final String FINISH_RETRY_EXTERNAL_CHANNEL_MESSAGE = "requestId = %s finish retry to External Channel";

    private final PaperRequestErrorDAO paperRequestErrorDAO;

    public NotRetriableWithoutSendErrorMessageHandler(PaperRequestErrorDAO paperRequestErrorDAO) {
        this.paperRequestErrorDAO = paperRequestErrorDAO;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        PnLogAudit pnLogAudit = new PnLogAudit();
        pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format(FINISH_RETRY_EXTERNAL_CHANNEL_MESSAGE, entity.getRequestId()));

        PnRequestError pnRequestError = PnRequestError.builder()
                .requestId(entity.getRequestId())
                .paId(entity.getRequestPaId())
                .error(entity.getStatusCode())
                .flowThrow(entity.getStatusDetail())
                .build();

        return paperRequestErrorDAO.created(pnRequestError)
                .map(requestError -> {
                    pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format(FINISH_RETRY_EXTERNAL_CHANNEL_MESSAGE, entity.getRequestId()));
                    return requestError;
                }).then();
    }
}
