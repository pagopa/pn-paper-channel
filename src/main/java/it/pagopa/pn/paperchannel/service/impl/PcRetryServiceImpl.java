package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.PcRetryService;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;

@Component
@RequiredArgsConstructor
public class PcRetryServiceImpl implements PcRetryService {

    private final PcRetryUtils pcRetryUtils;
    private final RequestDeliveryDAO requestDeliveryDAO;

    @Override
    public Mono<PcRetryResponse> getPcRetry(String requestId, Boolean checkApplyRasterization) {
        return requestDeliveryDAO.getByRequestId(getPrefixRequestId(requestId))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)))
                .flatMap(pnDeliveryRequest -> {
                    if (Boolean.TRUE.equals(pnDeliveryRequest.getApplyRasterization()) && Boolean.TRUE.equals(checkApplyRasterization)) {
                        return Mono.just(new PcRetryResponse().parentRequestId(requestId).retryFound(Boolean.FALSE));
                    }
                    return updateApplyRasterizationIfNeeded(requestId, checkApplyRasterization, pnDeliveryRequest)
                                    .flatMap(deliveryRequest -> pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse(requestId, deliveryRequest));
                });
    }

    private Mono<PnDeliveryRequest> updateApplyRasterizationIfNeeded(String requestId, Boolean checkApplyRasterization, PnDeliveryRequest pnDeliveryRequest) {
        if (Boolean.TRUE.equals(checkApplyRasterization)) {
            pnDeliveryRequest.setApplyRasterization(Boolean.TRUE);
            return requestDeliveryDAO.updateApplyRasterization(getPrefixRequestId(requestId), pnDeliveryRequest.getApplyRasterization())
                    .thenReturn(pnDeliveryRequest);
        }
        return Mono.just(pnDeliveryRequest);
    }

    public String getPrefixRequestId(String requestId) {
        requestId = Utility.getRequestIdWithoutPrefixClientId(requestId);
        if (requestId.contains(Const.RETRY)) {
            requestId = requestId.substring(0, requestId.indexOf(Const.RETRY));
        }
        return requestId;
    }
}
