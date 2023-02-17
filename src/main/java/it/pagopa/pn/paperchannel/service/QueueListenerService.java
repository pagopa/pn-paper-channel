package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessageDto;

public interface QueueListenerService {

    void internalListener(PrepareAsyncRequest data, int attempt);
    void nationalRegistriesResponseListener(AddressSQSMessageDto body);
    void nationalRegistriesErrorListener(NationalRegistryError data, int attempt);
    void externalChannelListener(SingleStatusUpdateDto data, int attempt);

}
