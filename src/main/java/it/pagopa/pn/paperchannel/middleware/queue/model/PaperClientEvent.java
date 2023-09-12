package it.pagopa.pn.paperchannel.middleware.queue.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PaperChannelUpdate;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PaperClientEvent extends DeliveryPushEvent {
    @JsonProperty("clientId")
    private String clientId;

    public PaperClientEvent(GenericEventHeader header, PaperChannelUpdate payload) {
        super(header, payload);
    }
}
