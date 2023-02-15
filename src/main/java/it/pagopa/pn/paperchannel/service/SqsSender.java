package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;

import java.time.Instant;


public interface SqsSender {

    void pushSendEvent(SendEvent event);
    void pushPrepareEvent(PrepareEvent event);
    void pushToInternalQueue(PrepareAsyncRequest prepareAsyncRequest);

    <T> void pushInternalError(T entity, int attempt, Class<T> tClass);
    <T> void rePushInternalError(T entity, int attempt, Instant expired, Class<T> tClass);
}
