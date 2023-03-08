package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class NotRetryableMessageHandler implements MessageHandler {

    private final PaperRequestErrorDAO paperRequestErrorDAO;

    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);
        pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry to External Channel", entity.getRequestId()));

        paperRequestErrorDAO.created(entity.getRequestId(), entity.getStatusCode(), entity.getStatusDetail())
                .doOnSuccess(pnRequestError -> pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry to External Channel", entity.getRequestId())))
                .subscribe();
    }

}
