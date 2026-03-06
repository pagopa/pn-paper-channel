package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.PaperTrackerClient;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PcRetryUtils {

    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final ExternalChannelClient externalChannelClient;
    private final AddressDAO addressDAO;
    private final PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;
    private final PaperTrackerClient paperTrackerClient;
    private final PaperTenderService paperTenderService;

    private static final String REQUEST_TO_EXTERNAL_CHANNEL = "prepare requestId = %s, trace_id = %s  request to External Channel";

    public boolean hasOtherAttempt(String requestId) {
        return pnPaperChannelConfig.getMaxPcRetry() == -1 || pnPaperChannelConfig.getMaxPcRetry() >= getRetryAttempt(requestId);
    }

    private int getRetryAttempt(String requestId) {
        int retry = 0;
        if (requestId.contains(Const.RETRY)) {
            retry = Integer.parseInt(requestId.substring(requestId.lastIndexOf("_") + 1));
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

    public Mono<PcRetryResponse> checkHasOtherAttemptAndMapPcRetryResponse(String requestId, PnDeliveryRequest pnDeliveryRequest) {
        PcRetryResponse pcRetryResponse = new PcRetryResponse();
        pcRetryResponse.setParentRequestId(requestId);

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
        log.info("SendEngage for status code {}", pnDeliveryRequest.getStatusCode());
        return addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId())
                .flatMap(pnAddresses -> callExternalChannel(pnAddresses, pnDeliveryRequest, requestId));
    }

    private Mono<PnDeliveryRequest> callExternalChannel(List<PnAddress> pnAddresses, PnDeliveryRequest pnDeliveryRequest, String requestId) {
        PnLogAudit pnLogAudit = new PnLogAudit();

        SendRequest sendRequest = SendRequestMapper.toDto(pnAddresses, pnDeliveryRequest);
        sendRequest.setRequestId(requestId);
        pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));

        List<AttachmentInfo> attachmentInfos = pnDeliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList();

        String pcRetry = String.valueOf(getRetryAttempt(requestId));

        return callInitTrackingAndEcSendEngage(pnDeliveryRequest.getRequestId(), sendRequest, attachmentInfos, pnDeliveryRequest, pcRetry)
                .doOnSuccess(unused -> pnLogAudit.addsSuccessSend(sendRequest.getIun(),
                        String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)))
                )
                .doOnError(ex ->
                        pnLogAudit.addsWarningSend(sendRequest.getIun(), String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)))
                )
                .thenReturn(pnDeliveryRequest);

    }

    public Mono<Void> callInitTrackingAndEcSendEngage(String requestId, SendRequest sendRequest, List<AttachmentInfo> attachmentInfos,
                                                      PnDeliveryRequest pnDeliveryRequest, String pcRetry) {
        if (pnPaperChannelConfig.getPaperTrackerProductList().contains(pnDeliveryRequest.getProductType())) {
            return retrieveUnifiedDeliveryDriver(sendRequest, requestId, pnDeliveryRequest.getProductType())
                    .map(PaperChannelDeliveryDriver::getUnifiedDeliveryDriver)
                    .flatMap(unifiedDeliveryDriver -> paperTrackerClient.initPaperTracking(
                                    requestId,
                                    Const.PCRETRY.concat(pcRetry),
                                    pnDeliveryRequest.getProductType(),
                                    unifiedDeliveryDriver)
                            .doOnSuccess(r -> log.debug("initPaperTracking done"))
                            .doOnError(ex -> log.error("Error on initPaperTracking: {}", ex.getMessage(), ex))
                    )
                    .thenReturn(sendRequest)
                    .flatMap(sendReq -> externalChannelClient.sendEngageRequest(sendReq, attachmentInfos, pnDeliveryRequest.getApplyRasterization()));
        }
        return externalChannelClient.sendEngageRequest(sendRequest, attachmentInfos, pnDeliveryRequest.getApplyRasterization());
    }

    private Mono<PaperChannelDeliveryDriver> retrieveUnifiedDeliveryDriver(SendRequest sendRequest, String requestId, String productType) {
        var address = sendRequest.getReceiverAddress();
        if(address == null) {
            log.error("Address is null for requestId {}", requestId);
            return Mono.error((new IllegalArgumentException("Address is null for requestId " + requestId)));
        }
        boolean isNational = Utility.isNational(address.getCountry());
        String geokey = (isNational) ? address.getCap() : address.getCountry();
        if (geokey == null) {
            log.error("GEOKEY is null for requestId {}", requestId);
            return Mono.error((new IllegalArgumentException("GEOKEY is null for requestId " + requestId)));
        }
        return paperTenderService.getSimplifiedCost(geokey, productType)
                .flatMap(cost -> paperChannelDeliveryDriverDAO.getByDeliveryDriverId(cost.getDeliveryDriverId()));
    }
}
