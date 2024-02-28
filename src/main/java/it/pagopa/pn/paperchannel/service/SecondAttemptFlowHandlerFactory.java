package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import org.springframework.stereotype.Component;

@Component
public class SecondAttemptFlowHandlerFactory {

    protected final PnLogAudit pnLogAudit;

    protected final PnPaperChannelConfig paperProperties;

    private final PstSecondAttemptFlowService pstSecondAttemptFlowService;

    private final NrgSecondAttemptFlowService nrgSecondAttemptFlowService;


    public SecondAttemptFlowHandlerFactory(AddressManagerClient addressManagerClient, PnAuditLogBuilder auditLogBuilder, PnPaperChannelConfig paperProperties) {
        this.pnLogAudit = new PnLogAudit(auditLogBuilder);
        this.paperProperties = paperProperties;
        this.pstSecondAttemptFlowService = new PstSecondAttemptFlowService(addressManagerClient, pnLogAudit, paperProperties);
        this.nrgSecondAttemptFlowService = new NrgSecondAttemptFlowService(addressManagerClient, pnLogAudit, paperProperties);
    }


    public SecondAttemptFlowService getSecondAttemptFlowService(FlowType flowType) {
        return switch (flowType) {
            case POSTMAN_FLOW -> pstSecondAttemptFlowService;
            case NATIONA_REGISTY_FLOW -> nrgSecondAttemptFlowService;
        };
    }


    public enum FlowType {
        NATIONA_REGISTY_FLOW,
        POSTMAN_FLOW;
    }
}
