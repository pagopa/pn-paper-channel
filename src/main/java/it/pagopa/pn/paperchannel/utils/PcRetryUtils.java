package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PcRetryUtils {

    private final PnPaperChannelConfig pnPaperChannelConfig;

    public boolean hasOtherAttempt(String requestId) {
        return pnPaperChannelConfig.getMaxPcRetry() == -1 || pnPaperChannelConfig.getMaxPcRetry() >= getRetryAttempt(requestId);
    }

    private int getRetryAttempt(String requestId) {
        int retry = 0;
        if (requestId.contains(Const.RETRY)) {
            retry = Integer.parseInt(requestId.substring(requestId.lastIndexOf("_")+1));
        }
        return retry;
    }

    public String setRetryRequestId(String requestId) {
        if (requestId.contains(Const.RETRY)) {
            String prefix = requestId.substring(0, requestId.indexOf(Const.RETRY));
            String attempt = String.valueOf(getRetryAttempt(requestId)+1);
            requestId = prefix.concat(Const.RETRY).concat(attempt);
        }
        return requestId;
    }


    private void setRetryRequestIdAndPcRetry(PcRetryResponse pcRetryResponse) {
        if (pcRetryResponse.getParentRequestId().contains(Const.RETRY)) {
            String prefix = pcRetryResponse.getParentRequestId().substring(0, pcRetryResponse.getParentRequestId().indexOf(Const.RETRY));
            String attempt = String.valueOf(getRetryAttempt(pcRetryResponse.getParentRequestId())+1);
            pcRetryResponse.setPcRetry(Const.RETRY.replaceFirst("^\\.", "").concat(attempt));
            pcRetryResponse.setRequestId(prefix.concat(Const.RETRY).concat(attempt));
        }
    }

    public PcRetryResponse checkHasOtherAttemptAndMapPcRetryResponse(String requestId, String unifiedDeliveryDriver) {
        PcRetryResponse pcRetryResponse = new PcRetryResponse();
        pcRetryResponse.setParentRequestId(requestId);
        pcRetryResponse.setDeliveryDriverId(unifiedDeliveryDriver);
        if (hasOtherAttempt(requestId)) {
            pcRetryResponse.setRetryFound(true);
            setRetryRequestIdAndPcRetry(pcRetryResponse);
        } else {
            pcRetryResponse.setRetryFound(false);
        }
        return pcRetryResponse;
    }
}
