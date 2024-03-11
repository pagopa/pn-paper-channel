package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import reactor.core.publisher.Mono;


public class NotRetryableErrorMessageHandler extends SendToDeliveryPushHandler {

    private final PaperRequestErrorDAO paperRequestErrorDAO;

    public NotRetryableErrorMessageHandler(SqsSender sqsSender, PaperRequestErrorDAO paperRequestErrorDAO) {
        super(sqsSender);
        this.paperRequestErrorDAO = paperRequestErrorDAO;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        PnLogAudit pnLogAudit = new PnLogAudit();
        pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry to External Channel", entity.getRequestId()));
        return super.handleMessage(entity, paperRequest)
                .then(Mono.defer(() ->
                    paperRequestErrorDAO.created(entity.getRequestId(), entity.getStatusCode(), entity.getStatusDetail())
                        .doOnSuccess(requestError ->
                            pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry to External Channel", entity.getRequestId()))
                        )
                ))
                .doOnError(throwable -> pnLogAudit.addsFailDiscard(entity.getIun(), String.format("requestId = %s finish retry to External Channel", entity.getRequestId())))
                .then();

    }

}
