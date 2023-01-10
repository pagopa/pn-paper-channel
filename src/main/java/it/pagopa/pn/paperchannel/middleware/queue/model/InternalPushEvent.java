package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InternalPushEvent implements GenericEvent<GenericEventHeader, PrepareAsyncRequest> {

    private GenericEventHeader header;
    private PrepareAsyncRequest payload;



    @Override
    public GenericEventHeader getHeader() {
        return header;
    }

    @Override
    public PrepareAsyncRequest getPayload() {
        return payload;
    }
}
