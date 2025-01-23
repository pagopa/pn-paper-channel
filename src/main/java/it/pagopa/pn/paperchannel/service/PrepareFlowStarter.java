package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;

public interface PrepareFlowStarter {

    void startPreparePhaseOneFromPrepareSync(PnDeliveryRequest deliveryRequest, String clientId);
    void startPreparePhaseOneFromNationalRegistriesFlow(PnDeliveryRequest deliveryRequest, Address nationalRegistriesAddress);
    void pushPreparePhaseOneOutput(PnDeliveryRequest deliveryRequest, PnAddress recipientNormalizedAddress);
    void redrivePreparePhaseOneAfterNationalRegistryError(NationalRegistryError entity, int attemptRetry);
    void redrivePreparePhaseOneAfterAddressManagerError(PnDeliveryRequest deliveryRequest, int attemptRetry, Address fromNationalRegistry);
    void redrivePreparePhaseTwoAfterF24Flow(PnDeliveryRequest deliveryRequest);
    void pushResultPrepareEvent(PnDeliveryRequest request, Address address, String clientId, StatusCodeEnum statusCode, KOReason koReason);
}
