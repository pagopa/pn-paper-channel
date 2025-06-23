package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AttemptPushEvent<T> implements GenericEvent<AttemptEventHeader, T> {

    private AttemptEventHeader header;
    private T payload;



    @Override
    public AttemptEventHeader getHeader() {
        return header;
    }

    @Override
    public T getPayload() {
        return payload;
    }
}
