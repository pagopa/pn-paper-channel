package it.pagopa.pn.paperchannel.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PrepareDelayEvent {

    private String requestId;
    private String iun;
    private Address recipientNormalizedAddress;
}
