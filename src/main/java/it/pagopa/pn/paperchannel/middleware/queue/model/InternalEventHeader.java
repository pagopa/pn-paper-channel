package it.pagopa.pn.paperchannel.middleware.queue.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
public class InternalEventHeader extends GenericEventHeader {

    public static final String PN_EVENT_HEADER_ATTEMPT = "attempt";
    public static final String PN_EVENT_HEADER_EXPIRED = "expired";

    @JsonProperty("attempt")
    private Integer attempt = 0;
    @JsonProperty("expired")
    private Instant expired;


}