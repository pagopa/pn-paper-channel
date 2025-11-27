package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEvent;
import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.model.*;

public interface QueueListenerService {

    void internalListener(PrepareAsyncRequest data, int attempt);
    void normalizeAddressListener(PrepareNormalizeAddressEvent data, int attempt);
    void nationalRegistriesResponseListener(AddressSQSMessageDto body);
    void nationalRegistriesErrorListener(NationalRegistryError data, int attempt);
    void externalChannelListener(SingleStatusUpdateDto data, int attempt);
    void manualRetryExternalChannel(String requestId, String newPcRetry);
    void f24ErrorListener(F24Error entity, Integer attempt);
    void f24ResponseListener(PnF24PdfSetReadyEvent.Detail body);
    void delayerListener(PnPrepareDelayerToPaperchannelPayload data, int attempt);

}
