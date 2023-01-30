package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalPushEvent<T> implements GenericEvent<GenericEventHeader, T> {

    private GenericEventHeader header;
    private T payload;



    @Override
    public GenericEventHeader getHeader() {
        return header;
    }

    @Override
    public T getPayload() {
        return payload;
    }
}
