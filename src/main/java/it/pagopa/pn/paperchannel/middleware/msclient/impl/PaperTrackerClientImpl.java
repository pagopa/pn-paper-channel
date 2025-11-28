package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.NotificationReworkApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.PaperTrackerTrackingApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.dto.TrackingCreationRequestDto;
import it.pagopa.pn.paperchannel.middleware.msclient.PaperTrackerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.PAPER_TRACKER_REQUEST_CONFLICT;

@Component
@RequiredArgsConstructor
public class PaperTrackerClientImpl implements PaperTrackerClient {

    private final PaperTrackerTrackingApi paperTrackerEventApi;
    private final NotificationReworkApi notificationReworkApi;

    public Mono<Void> initPaperTracking(String attemptId, String pcRetry, String productType, String unifiedDeliveryDriver){
        TrackingCreationRequestDto trackerCreationRequestDto = new TrackingCreationRequestDto();
        trackerCreationRequestDto.setAttemptId(attemptId);
        trackerCreationRequestDto.setPcRetry(pcRetry);
        trackerCreationRequestDto.setProductType(productType);
        trackerCreationRequestDto.setUnifiedDeliveryDriver(unifiedDeliveryDriver);
        return paperTrackerEventApi.initTracking(trackerCreationRequestDto)
                .onErrorMap(ex -> {
                    if (ex instanceof WebClientResponseException exception && exception.getStatusCode() == HttpStatus.CONFLICT) {
                        return new PnIdConflictException(PAPER_TRACKER_REQUEST_CONFLICT.getTitle(), Map.of("trackingId", String.join(".", attemptId, pcRetry)));
                    }
                    return ex;
                });
    }

    @Override
    public Mono<Void> initNotificationRework(String reworkId, String requestId) {
        return notificationReworkApi.initNotificationRework(reworkId, requestId);
    }

}
