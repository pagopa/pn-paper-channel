package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.PaperTrackerClient;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.NotificationReworkService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;

@CustomLog
@Service
@RequiredArgsConstructor
public class NotificationReworkServiceImpl implements NotificationReworkService {

    private final PcRetryServiceImpl pcRetryService;
    private final MetaDematCleaner metaDematCleaner;
    private final RequestDeliveryDAO requestDeliveryDAO;
    private final PaperTrackerClient paperTrackerClient;
    private final ExternalChannelClient externalChannelClient;

    @Override
    public Mono<Void> initNotificationRework(String requestId, String reworkId) {
        String requestIdWithoutPcRetry = pcRetryService.getPrefixRequestId(requestId);

        return metaDematCleaner.clean(requestIdWithoutPcRetry)
                .then(requestDeliveryDAO.getByRequestId(requestIdWithoutPcRetry))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)))
                .flatMap(deliveryRequest -> requestDeliveryDAO.cleanDataForNotificationRework(deliveryRequest, reworkId))
                .flatMap(deliveryRequest -> paperTrackerClient.initNotificationRework(reworkId, requestId))
                .doOnError(error -> log.error("Error in initNotificationRework for requestId: {} and reworkId: {}", requestId, reworkId, error))
                .thenReturn(requestId)
                .flatMap(s -> externalChannelClient.initNotificationRework(requestId))
                .doOnError(error -> log.error("Error in patchRequestMetadata for requestId: {}", requestId, error));
    }
}