package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsRequestEntry;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@CustomLog
@Component
public class EventBridgeProducer {
    private final AmazonEventBridgeAsync amazonEventBridge;
    private final String eventBusName;
    private final String eventBusDetailType;
    private final String eventBusSource;

    public EventBridgeProducer(AmazonEventBridgeAsync amazonEventBridge,
                        @Value("${pn.paper-channel.eventbus.name}") String eventBusName,
                        @Value("${pn.paper-channel.eventbus.source}") String eventBusSource,
                        @Value("${pn.paper-channel.eventbus.detail.type}") String eventBusDetailType) {
        this.amazonEventBridge = amazonEventBridge;
        this.eventBusName = eventBusName;
        this.eventBusSource = eventBusSource;
        this.eventBusDetailType = eventBusDetailType;
    }

    public void sendEvent(String message, String requestId) {
        amazonEventBridge.putEventsAsync(putEventsRequestBuilder(message),
                new AsyncHandler<>() {
                    @Override
                    public void onError(Exception e) {
                        log.error("Send event with requestId {} failed", requestId, e);
                    }

                    @Override
                    public void onSuccess(PutEventsRequest request, PutEventsResult putEventsResult) {
                        log.info("Event with requestId {} sent successfully", requestId);
                        log.debug("Sent event result: {}", putEventsResult.getEntries());
                    }
                });
    }

    private PutEventsRequest putEventsRequestBuilder(String message) {
        PutEventsRequest putEventsRequest = new PutEventsRequest();
        List<PutEventsRequestEntry> entries = new ArrayList<>();
        PutEventsRequestEntry entryObj = new PutEventsRequestEntry();
        entryObj.setDetail(message);
        entryObj.setEventBusName(eventBusName);
        entryObj.setDetailType(eventBusDetailType);
        entryObj.setSource(eventBusSource);
        entries.add(entryObj);
        putEventsRequest.setEntries(entries);
        log.debug("PutEventsRequest: {}", putEventsRequest);
        return putEventsRequest;
    }




}
