package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.service.impl.NrgSecondAttemptFlowService;
import it.pagopa.pn.paperchannel.service.impl.PstSecondAttemptFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecondAttemptFlowHandlerFactory {

    protected final PnPaperChannelConfig paperProperties;

    private final PstSecondAttemptFlowService pstSecondAttemptFlowService;

    private final NrgSecondAttemptFlowService nrgSecondAttemptFlowService;


    public SecondAttemptFlowHandlerFactory(AddressManagerClient addressManagerClient, PnPaperChannelConfig paperProperties) {
        this.paperProperties = paperProperties;
        this.pstSecondAttemptFlowService = new PstSecondAttemptFlowService(addressManagerClient, paperProperties);
        this.nrgSecondAttemptFlowService = new NrgSecondAttemptFlowService(addressManagerClient, paperProperties);
    }


    public SecondAttemptFlowService getSecondAttemptFlowService(FlowType flowType) {
        log.info("Flow type: {}", flowType);
        return switch (flowType) {
            case POSTMAN_FLOW -> pstSecondAttemptFlowService;
            case NATIONAL_REGISTY_FLOW -> nrgSecondAttemptFlowService;
        };
    }


    public enum FlowType {
        NATIONAL_REGISTY_FLOW,
        POSTMAN_FLOW;
    }
}
