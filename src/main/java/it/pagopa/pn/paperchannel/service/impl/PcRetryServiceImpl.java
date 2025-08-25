package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.PcRetryService;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_DRIVER_NOT_EXISTED;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;

@Component
@RequiredArgsConstructor
public class PcRetryServiceImpl implements PcRetryService {

    private final PcRetryUtils pcRetryUtils;
    private final RequestDeliveryDAO requestDeliveryDAO;
    private final PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;

    @Override
    public Mono<PcRetryResponse> getPcRetry(String requestId) {
        return requestDeliveryDAO.getByRequestId(getPrefixRequestId(requestId))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)))
                .map(PnDeliveryRequest::getDriverCode)
                .flatMap(paperChannelDeliveryDriverDAO::getByDeliveryDriverId)
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage(), HttpStatus.NOT_FOUND)))
                .map(PaperChannelDeliveryDriver::getUnifiedDeliveryDriver)
                .map(unifiedDeliveryDriver -> pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse(requestId, unifiedDeliveryDriver));
    }

    private String getPrefixRequestId(String requestId) {
        requestId = Utility.getRequestIdWithoutPrefixClientId(requestId);
        if (requestId.contains(Const.RETRY)) {
            requestId = requestId.substring(0, requestId.indexOf(Const.RETRY));
        }
        return requestId;
    }
}
