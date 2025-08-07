package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OcrInputEvent implements GenericEvent<GenericEventHeader, OcrInputPayload> {

    private GenericEventHeader header;
    private OcrInputPayload payload;

    @Override
    public GenericEventHeader getHeader() {
        return header;
    }

    @Override
    public OcrInputPayload getPayload() {
        return payload;
    }
}