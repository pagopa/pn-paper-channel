package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;

public interface QueueListenerService {

    void internalListener(PrepareAsyncRequest data, int attempt);
    void nationalRegistriesResponseListener(AddressSQSMessageDto body);
    void nationalRegistriesErrorListener(NationalRegistryError data, int attempt);
    void externalChannelListener(SingleStatusUpdateDto data, int attempt);
    void manualRetryExternalChannel(String requestId);

}
