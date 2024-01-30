package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.model.DematZipInternalEvent;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;


import java.time.Instant;


public interface SqsSender {

    void pushSendEvent(SendEvent event);
    void pushPrepareEvent(PrepareEvent event);
    void pushToInternalQueue(PrepareAsyncRequest prepareAsyncRequest);
    void pushDematZipInternalEvent(DematZipInternalEvent dematZipInternalEvent);

    void pushSendEventOnEventBridge(String clientId, SendEvent event);
    void pushPrepareEventOnEventBridge(String clientId, PrepareEvent event);

    <T> void pushInternalError(T entity, int attempt, Class<T> tClass);
    <T> void rePushInternalError(T entity, int attempt, Instant expired, Class<T> tClass);
}
