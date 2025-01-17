package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;

public interface PrepareFlowStarter {

    void startPreparePhaseOneFromPrepareSync(PnDeliveryRequest deliveryRequest, String clientId);
    void startPreparePhaseOneFromNationalRegistriesFlow(PnDeliveryRequest deliveryRequest, Address nationalRegistriesAddress);
    void redrivePreparePhaseOneAfterNationalRegistryError(NationalRegistryError entity, int attemptRetry);
    void redrivePreparePhaseOneAfterAddressManagerError(PnDeliveryRequest deliveryRequest, int attemptRetry);
}
