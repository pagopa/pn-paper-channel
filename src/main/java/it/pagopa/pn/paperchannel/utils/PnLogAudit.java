package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;

public class PnLogAudit {

    private PnAuditLogBuilder pnAuditLogBuilder;

    public PnLogAudit(PnAuditLogBuilder pnAuditLogBuilder) {
        this.pnAuditLogBuilder = pnAuditLogBuilder;
    }

    public void addsResolveLogic(String iun, String msg1, String msg2) {
        addsBeforeResolveLogic(iun, msg1);
        addsSuccessResolveLogic(iun, msg2);
    }

    public void addsBeforeResolveLogic(String iun, String msg) {
        PnAuditLogEvent pnAuditLogEvent = pnAuditLogBuilder.before(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, msg)
                .iun(iun)
                .build();
        pnAuditLogEvent.log();
    }

    public void addsSuccessResolveLogic(String iun, String successMsg) {
        pnAuditLogBuilder.iun(iun).build().generateSuccess(successMsg);
    }

}
