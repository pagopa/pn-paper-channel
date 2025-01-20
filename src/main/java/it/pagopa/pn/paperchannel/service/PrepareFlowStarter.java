package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;

public interface PrepareFlowStarter {

    void startPreparePhaseOneFromPrepareSync(PnDeliveryRequest deliveryRequest, String clientId);
    void startPreparePhaseOneFromNationalRegistriesFlow(PnDeliveryRequest deliveryRequest, Address nationalRegistriesAddress);
}
