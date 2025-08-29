package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PcRetryUtils {

    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final ExternalChannelClient externalChannelClient;
    private final AddressDAO addressDAO;

    private static final String REQUEST_TO_EXTERNAL_CHANNEL = "prepare requestId = %s, trace_id = %s  request to External Channel";

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
            String attempt = String.valueOf(getRetryAttempt(requestId) + 1);
            requestId = prefix.concat(Const.RETRY).concat(attempt);
        }
        return requestId;
    }


    private void setRetryRequestIdAndPcRetry(PcRetryResponse pcRetryResponse, String newRequestId) {
        String suffix = newRequestId.substring(newRequestId.indexOf(Const.PCRETRY), newRequestId.length());
        pcRetryResponse.setPcRetry(suffix);
        pcRetryResponse.setRequestId(newRequestId);
    }

    public Mono<PcRetryResponse> checkHasOtherAttemptAndMapPcRetryResponse(String requestId, String unifiedDeliveryDriver, PnDeliveryRequest pnDeliveryRequest) {
        PcRetryResponse pcRetryResponse = new PcRetryResponse();
        pcRetryResponse.setParentRequestId(requestId);
        pcRetryResponse.setDeliveryDriverId(unifiedDeliveryDriver);

        if (hasOtherAttempt(requestId)) {
            pcRetryResponse.setRetryFound(true);
            String newRequestId = setRetryRequestId(requestId);
            setRetryRequestIdAndPcRetry(pcRetryResponse, newRequestId);
            return sendEngageRequest(pnDeliveryRequest, newRequestId).thenReturn(pcRetryResponse);
        }

        pcRetryResponse.setRetryFound(false);
        return Mono.just(pcRetryResponse);
    }

    public Mono<PnDeliveryRequest> sendEngageRequest(PnDeliveryRequest pnDeliveryRequest, String requestId) {
        List<String> retryProducts = pnPaperChannelConfig.getPaperTrackerOnRetrySendEngageProducts();

        if (!CollectionUtils.isEmpty(retryProducts) && retryProducts.contains(pnDeliveryRequest.getProductType()))
            return addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId())
                    .flatMap(pnAddresses -> callExternalChannel(pnAddresses, pnDeliveryRequest, requestId));

        return Mono.empty();
    }

    private Mono<PnDeliveryRequest> callExternalChannel(List<PnAddress> pnAddresses, PnDeliveryRequest pnDeliveryRequest, String requestId) {
        PnLogAudit pnLogAudit = new PnLogAudit();

        SendRequest sendRequest = SendRequestMapper.toDto(pnAddresses, pnDeliveryRequest);
        sendRequest.setRequestId(requestId);
        pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));

        List<AttachmentInfo> attachmentInfos = pnDeliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList();

        return externalChannelClient.sendEngageRequest(sendRequest, attachmentInfos, pnDeliveryRequest.getApplyRasterization())
                .doOnSuccess(unused -> pnLogAudit.addsSuccessSend(sendRequest.getIun(),
                        String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)))
                )
                .doOnError(ex ->
                        pnLogAudit.addsWarningSend(sendRequest.getIun(), String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)))
                )
                .thenReturn(pnDeliveryRequest);

    }
}
