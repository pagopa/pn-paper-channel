package it.pagopa.pn.paperchannel.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DelayerToPaperChannelEventPayload {
    private String requestId;
    private String iun;
    private int attemptRetry;
    private String clientId;
}
