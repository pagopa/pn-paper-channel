package it.pagopa.pn.paperchannel.middleware.queue.producer;

import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

@CustomLog
@Component
public class EventBridgeProducer {
    private final EventBridgeAsyncClient amazonEventBridge;
    private final String eventBusName;
    private final String eventBusDetailType;
    private final String eventBusSource;

    public EventBridgeProducer(EventBridgeAsyncClient amazonEventBridge,
                        @Value("${pn.paper-channel.eventbus.name}") String eventBusName,
                        @Value("${pn.paper-channel.eventbus.source}") String eventBusSource,
                        @Value("${pn.paper-channel.eventbus.detail.type}") String eventBusDetailType) {
        this.amazonEventBridge = amazonEventBridge;
        this.eventBusName = eventBusName;
        this.eventBusSource = eventBusSource;
        this.eventBusDetailType = eventBusDetailType;
    }

    public void sendEvent(String message, String requestId) {
        amazonEventBridge.putEvents(putEventsRequestBuilder(message))
                .whenComplete((putEventsResponse, throwable) -> {
                    if (throwable != null) {
                        log.error("Send event with requestId {} failed", requestId, throwable);
                    } else {
                        log.info("Event with requestId {} sent successfully", requestId);
                        log.debug("Sent event result: {}", putEventsResponse.entries());
                    }
                });
    }

    private PutEventsRequest putEventsRequestBuilder(String message) {
        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .detail(message)
                .eventBusName(eventBusName)
                .detailType(eventBusDetailType)
                .source(eventBusSource)
                .build();

        PutEventsRequest putEventsRequest = PutEventsRequest.builder()
                .entries(entry)
                .build();

        log.debug("PutEventsRequest: {}", putEventsRequest);
        return putEventsRequest;
    }


}
