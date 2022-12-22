package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PrepareDeliveryEvent implements GenericEvent<GenericEventHeader, PaperChannelUpdate> {

    private GenericEventHeader header;
    private PaperChannelUpdate payload;

    @Override
    public GenericEventHeader getHeader() {
        return header;
    }

    @Override
    public PaperChannelUpdate getPayload() {
        return payload;
    }
}
