package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.PaperTrackerTrackingApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.dto.TrackingCreationRequestDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
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

    public Mono<Void> initPaperTracking(String trackingId, String productType, String unifiedDeliveryDriver){
        TrackingCreationRequestDto trackerCreationRequestDto = new TrackingCreationRequestDto();
        trackerCreationRequestDto.setAttemptId(trackingId.split("\\.")[0]);
        trackerCreationRequestDto.setPcRetry(trackingId.split("\\.")[1]);
        trackerCreationRequestDto.setProductType(productType);
        trackerCreationRequestDto.setUnifiedDeliveryDriver(unifiedDeliveryDriver);
        return paperTrackerEventApi.initTracking(trackerCreationRequestDto)
                .onErrorMap(ex -> {
                    if (ex instanceof WebClientResponseException exception && exception.getStatusCode() == HttpStatus.CONFLICT) {
                        return new PnIdConflictException(PAPER_TRACKER_REQUEST_CONFLICT.getTitle(), Map.of("trackingId", trackingId));
                    }
                    return ex;
                });
    }

}
