package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeliveryEvent implements GenericEvent<GenericEventHeader, DeliveryPayload> {

    private GenericEventHeader header;
    private DeliveryPayload payload;

    @Override
    public GenericEventHeader getHeader() {
        return header;
    }

    @Override
    public DeliveryPayload getPayload() {
        return payload;
    }
}
