package it.pagopa.pn.paperchannel.middleware.queue.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;


@NoArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
public class InternalEventHeader extends AttemptEventHeader {

    private Instant expired;

    public InternalEventHeader(Integer attempt, Instant expired) {
        super(attempt);
        this.expired = expired;
    }

    public InternalEventHeader(Integer attempt, Instant expired, String clientId) {
        super(attempt, clientId);
        this.expired = expired;
    }

}