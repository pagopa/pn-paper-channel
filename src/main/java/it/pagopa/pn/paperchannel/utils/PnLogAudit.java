package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;

public class PnLogAudit {

    private PnAuditLogBuilder pnAuditLogBuilder;

    public PnLogAudit(PnAuditLogBuilder pnAuditLogBuilder) {
        this.pnAuditLogBuilder = pnAuditLogBuilder;
    }

    public void addsLog(PnAuditLogEventType type, String iun, String msg1, String msg2) {
        addsBeforeLog(type, iun, msg1);
        addsSuccessLog(iun, msg2);
    }

    public void addsBeforeLog(PnAuditLogEventType type, String iun, String msg) {
        PnAuditLogEvent pnAuditLogEvent = pnAuditLogBuilder.before(type, msg)
                .iun(iun)
                .build();
        pnAuditLogEvent.log();
    }

    public void addsSuccessLog(String iun, String successMsg) {
        pnAuditLogBuilder.iun(iun).build().generateSuccess(successMsg).log();
    }

    public void addsFailLog(String iun, String successMsg) {
        pnAuditLogBuilder.iun(iun).build().generateFailure(successMsg).log();
    }

    public void addsResolveLogic(String iun, String msg1, String msg2) {
        addsLog(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, iun, msg1, msg2);
    }

    public void addsBeforeResolveLogic(String iun, String msg) {
        addsBeforeLog(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, iun, msg);
    }

    public void addsBeforeResolveService(String iun, String msg) {
        addsBeforeLog(PnAuditLogEventType.AUD_FD_RESOLVE_SERVICE, iun, msg);
    }

}
