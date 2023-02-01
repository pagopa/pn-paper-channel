package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalPushEvent<T> implements GenericEvent<InternalEventHeader, T> {

    private InternalEventHeader header;
    private T payload;



    @Override
    public InternalEventHeader getHeader() {
        return header;
    }

    @Override
    public T getPayload() {
        return payload;
    }
}
