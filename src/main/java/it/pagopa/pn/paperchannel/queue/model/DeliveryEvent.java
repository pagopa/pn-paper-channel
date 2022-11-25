package it.pagopa.pn.paperchannel.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeliveryEvent implements GenericEvent<StandardEventHeader, DeliveryPayload> {

    private StandardEventHeader header;

    private DeliveryPayload payload;

    @Override
    public StandardEventHeader getHeader() {
        return header;
    }

    @Override
    public DeliveryPayload getPayload() {
        return payload;
    }
}
