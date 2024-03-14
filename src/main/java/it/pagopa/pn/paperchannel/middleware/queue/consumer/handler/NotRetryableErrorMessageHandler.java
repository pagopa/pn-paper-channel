package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import reactor.core.publisher.Mono;

public class NotRetryableErrorMessageHandler extends SendToDeliveryPushHandler {

    private static final String FINISH_RETRY_EXTERNAL_CHANNEL_MESSAGE = "requestId = %s finish retry to External Channel";

    private final PaperRequestErrorDAO paperRequestErrorDAO;

    public NotRetryableErrorMessageHandler(SqsSender sqsSender, PaperRequestErrorDAO paperRequestErrorDAO) {
        super(sqsSender);
        this.paperRequestErrorDAO = paperRequestErrorDAO;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        PnLogAudit pnLogAudit = new PnLogAudit();
        pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format(FINISH_RETRY_EXTERNAL_CHANNEL_MESSAGE, entity.getRequestId()));

        return super.handleMessage(entity, paperRequest)
                .doOnSuccess(pnRequestError -> {

                    PnRequestError requestError = PnRequestError.builder()
                            .requestId(entity.getRequestId())
                            .paId(entity.getRequestPaId())
                            .error(entity.getStatusCode())
                            .flowThrow(entity.getStatusDetail())
                            .build();

                    paperRequestErrorDAO.created(requestError);
                    pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format(FINISH_RETRY_EXTERNAL_CHANNEL_MESSAGE, entity.getRequestId()));
                })
                .doOnError(throwable -> pnLogAudit.addsFailDiscard(entity.getIun(), String.format(FINISH_RETRY_EXTERNAL_CHANNEL_MESSAGE, entity.getRequestId())))
                .then();
    }
}
