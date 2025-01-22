package it.pagopa.pn.paperchannel.middleware.queue.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.*;
import lombok.experimental.SuperBuilder;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
public class AttemptEventHeader extends GenericEventHeader {

    public static final String PN_EVENT_HEADER_ATTEMPT = "attempt";
    public static final String PN_EVENT_HEADER_CLIENT_ID = "x-client-id";


    private int attempt;
    @JsonProperty("x-client-id")
    private String clientId;

    public AttemptEventHeader(Integer attempt) {
        this.attempt = attempt;
    }

}