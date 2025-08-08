package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.api.PaperTrackerEventApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnpapertracker.v1.dto.TrackerCreationRequestDto;
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

    private final PaperTrackerEventApi paperTrackerEventApi;

    public Mono<PnDeliveryRequest> initPaperTracking(PnDeliveryRequest pnDeliveryRequest, String unifiedDeliveryDriver){
        TrackerCreationRequestDto trackerCreationRequestDto = new TrackerCreationRequestDto();
        trackerCreationRequestDto.setRequestId(pnDeliveryRequest.getRequestId());
        trackerCreationRequestDto.setProductType(pnDeliveryRequest.getProductType());
        trackerCreationRequestDto.setUnifiedDeliveryDriver(unifiedDeliveryDriver);
        return paperTrackerEventApi.initTracking(trackerCreationRequestDto)
                .onErrorMap(ex -> {
                    if (ex instanceof WebClientResponseException exception && exception.getStatusCode() == HttpStatus.CONFLICT) {
                        return new PnIdConflictException(PAPER_TRACKER_REQUEST_CONFLICT.getTitle(), Map.of("requestId", pnDeliveryRequest.getRequestId()));
                    }
                    return ex;
                })
                .thenReturn(pnDeliveryRequest);
    }

}
