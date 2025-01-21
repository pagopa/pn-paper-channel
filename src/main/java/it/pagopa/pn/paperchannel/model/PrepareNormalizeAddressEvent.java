package it.pagopa.pn.paperchannel.model;

import lombok.*;

/**
 * Model representing the input payload of PREPARE phase 1.
 */
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PrepareNormalizeAddressEvent {

    private String requestId;

    private String iun;

    private String correlationId;

    private Address address;

    private boolean isAddressRetry;

    private int attempt;

    private String clientId;
}
