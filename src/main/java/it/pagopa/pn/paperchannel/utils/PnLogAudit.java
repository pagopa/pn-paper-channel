package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEventType;

public class PnLogAudit {

    private PnAuditLogBuilder pnAuditLogBuilder;

    public PnLogAudit(PnAuditLogBuilder pnAuditLogBuilder) {
        this.pnAuditLogBuilder = pnAuditLogBuilder;
    }

    public void addsLog(PnAuditLogEventType type, String iun, String msg1, String msg2) {
        addsBeforeLog(type, iun, msg1);
        addsSuccessLog(type, iun, msg2);
    }

    public void addsBeforeLog(PnAuditLogEventType type, String iun, String msg) {
        pnAuditLogBuilder.before(type, msg)
                .iun(iun)
                .build().log();
    }

    public void addsSuccessLog(PnAuditLogEventType type, String iun, String successMsg) {
        pnAuditLogBuilder.before(type, successMsg)
                .iun(iun)
                .build().generateSuccess(successMsg).log();
    }

    public void addsFailLog(PnAuditLogEventType type, String iun, String successMsg) {
        pnAuditLogBuilder.before(type, successMsg).iun(iun).build().generateFailure(successMsg).log();
    }

    public void addsBeforeLogWithoutIun(PnAuditLogEventType type, String msg) {
        pnAuditLogBuilder.before(type, msg)
                .build().log();
    }

    public void addsSuccessLogWithoutIun(PnAuditLogEventType type, String successMsg) {
        pnAuditLogBuilder.before(type, successMsg)
                .build().generateSuccess(successMsg).log();
    }

    public void addsFailLogWithoutIun(PnAuditLogEventType type, String successMsg) {
        pnAuditLogBuilder.before(type, successMsg)
                .build()
                .generateFailure(successMsg).log();
    }

    public void addsResolveLogic(String iun, String msg1, String msg2) {
        addsLog(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, iun, msg1, msg2);
    }

    public void addsBeforeResolveLogic(String iun, String msg) {
        addsBeforeLog(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, iun, msg);
    }

    public void addsSuccessResolveLogic(String iun, String msg) {
        addsSuccessLog(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC, iun, msg);
    }

    public void addsBeforeResolveService(String iun, String msg) {
        addsBeforeLog(PnAuditLogEventType.AUD_FD_RESOLVE_SERVICE, iun, msg);
    }

    public void addsSuccessResolveService(String iun, String msg) {
        addsSuccessLog(PnAuditLogEventType.AUD_FD_RESOLVE_SERVICE, iun, msg);
    }

    public void addsFailResolveService(String iun, String msg) {
        addsFailLog(PnAuditLogEventType.AUD_FD_RESOLVE_SERVICE, iun, msg);
    }

    public void addsBeforeReceive(String iun, String msg) {
        addsBeforeLog(PnAuditLogEventType.AUD_FD_RECEIVE, iun, msg);
    }

    public void addsSuccessReceive(String iun, String msg) {
        addsSuccessLog(PnAuditLogEventType.AUD_FD_RECEIVE, iun, msg);
    }
    public void addsFailReceive(String iun, String msg) {
        addsFailLog(PnAuditLogEventType.AUD_FD_RECEIVE, iun, msg);
    }

    public void addsBeforeCreate(String msg) {
        addsBeforeLogWithoutIun(PnAuditLogEventType.AUD_DT_CREATE, msg);
    }

    public void addsSuccessCreate(String msg) {
        addsSuccessLogWithoutIun(PnAuditLogEventType.AUD_DT_CREATE, msg);
    }
    public void addsFailCreate(String msg) {
        addsFailLogWithoutIun(PnAuditLogEventType.AUD_DT_CREATE, msg);
    }

    public void addsBeforeUpdate(String msg) {
        addsBeforeLogWithoutIun(PnAuditLogEventType.AUD_DT_UPDATE, msg);
    }

    public Void addsSuccessUpdate(String msg) {
        addsSuccessLogWithoutIun(PnAuditLogEventType.AUD_DT_UPDATE, msg);
        return null;
    }

    public Void addsFailUpdate(String msg) {
        addsFailLogWithoutIun(PnAuditLogEventType.AUD_DT_UPDATE, msg);
        return null;
    }


    public void addsBeforeDelete(String msg) {
        addsBeforeLogWithoutIun(PnAuditLogEventType.AUD_DT_DELETE, msg);
    }

    public void addsSuccessDelete(String msg) {
        addsSuccessLogWithoutIun(PnAuditLogEventType.AUD_DT_DELETE, msg);
    }

    public void addsFailDelete(String msg) {
        addsFailLogWithoutIun(PnAuditLogEventType.AUD_DT_DELETE, msg);
    }



    public void addsBeforeSend(String iun, String msg) {
        addsBeforeLog(PnAuditLogEventType.AUD_FD_SEND, iun, msg);
    }

    public void addsSuccessSend(String iun, String msg) {
        addsSuccessLog(PnAuditLogEventType.AUD_FD_SEND, iun, msg);
    }

    public void addsFailSend(String iun, String msg) {
        addsFailLog(PnAuditLogEventType.AUD_FD_SEND, iun, msg);
    }

    public void addsBeforeDiscard(String iun, String msg) {
        addsBeforeLog(PnAuditLogEventType.AUD_FD_DISCARD, iun, msg);
    }

    public void addsSuccessDiscard(String iun, String msg) {
        addsSuccessLog(PnAuditLogEventType.AUD_FD_DISCARD, iun, msg);
    }

    public void addsFailDiscard(String iun, String msg) {
        addsFailLog(PnAuditLogEventType.AUD_FD_DISCARD, iun, msg);
    }


}
