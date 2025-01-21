package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.paperchannel.model.DelayerToPaperChannelEventPayload;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DelayerToPaperChannelEvent implements GenericEvent<InternalEventHeader, DelayerToPaperChannelEventPayload> {

    private InternalEventHeader header;
    private DelayerToPaperChannelEventPayload payload;

    @Override
    public InternalEventHeader getHeader() {
        return header;
    }

    @Override
    public DelayerToPaperChannelEventPayload getPayload() {
        return payload;
    }
}
